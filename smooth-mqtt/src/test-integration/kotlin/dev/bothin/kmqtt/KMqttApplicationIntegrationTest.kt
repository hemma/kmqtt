package dev.bothin.kmqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bothin.kmqtt.mqtt.InMessage
import dev.bothin.kmqtt.mqtt.KMqttClient
import dev.bothin.kmqtt.mqtt.OutMessage
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.awaitility.Duration
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class KMqttApplicationIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        private val mqttContainer = KGenericContainer("eclipse-mosquitto")
            .withExposedPorts(1883)
            .waitingFor(Wait.forLogMessage(".*Config loaded from.*", 1))
    }

    private lateinit var mqttClient: MqttClient

    private lateinit var kMqttClient: KMqttClient

    private lateinit var kmqtt: KMqtt

    @BeforeEach
    fun setUp() {
        mqttClient = MqttClient("tcp://${mqttContainer.containerIpAddress}:${mqttContainer.getMappedPort(1883)}", "clientID", MemoryPersistence())
        kMqttClient = KMqttClient(mqttClient, jacksonObjectMapper().findAndRegisterModules())
        kmqtt = KMqtt(kMqttClient)

        kMqttClient.connect()
    }

    @Test
    fun `when sub then subscribe to topic`() {
        val expectedMsg = Dto(msg = "msg")
        val app = KMqttApplication(kMqttClient)

        var msgIn: Dto? = null
        app {
            subscribe("topicIn") { msg: InMessage<Dto> ->
                msgIn = msg.payload
                OutMessage.nothing()
            }
        }

        kmqtt.emit("topicIn", expectedMsg.copy())

        await atMost Duration.FIVE_HUNDRED_MILLISECONDS untilNotNull {
            msgIn
        }
        msgIn shouldBe expectedMsg
    }

    @Test
    fun `when sub then emit to topic`() {
        val expectedMsg = Dto(msg = "new_msg")
        val app = KMqttApplication(kMqttClient)

        app {
            subscribe("topicIn", "topicOut") { _: InMessage<Dto> ->
                OutMessage(expectedMsg.copy())
            }
        }

        val response = runBlocking {
            kmqtt.emitWaitReceive("topicIn", Dto(msg = "msg"), "topicOut", java.time.Duration.ofMillis(500))
        }

        response shouldBe expectedMsg
    }

}

internal data class Dto(val msg: String)