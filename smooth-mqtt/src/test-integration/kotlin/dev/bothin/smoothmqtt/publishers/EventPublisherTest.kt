package dev.bothin.smoothmqtt.publishers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bothin.smoothmqtt.KGenericContainer
import dev.bothin.smoothmqtt.event.EventApplication
import dev.bothin.smoothmqtt.event.EventProducer
import dev.bothin.smoothmqtt.event.EventPublisher
import io.mockk.junit5.MockKExtension
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kodein.di.direct
import org.kodein.di.generic.instance
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@ExtendWith(MockKExtension::class)
@Testcontainers
class EventPublisherTest {

    @Container
    private val mqttContainer = KGenericContainer("eclipse-mosquitto")
        .withExposedPorts(1883)
        .waitingFor(Wait.forLogMessage(".*Config loaded from.*", 1))


    private lateinit var mqttClient: MqttClient

    private val mapper = jacksonObjectMapper()

    @BeforeEach
    fun setup() {
        mqttClient = MqttClient("tcp://${mqttContainer.containerIpAddress}:${mqttContainer.getMappedPort(1883)}", "clientTest")
        mqttClient.connect()
    }

    @Test
    fun `when produce annotation then publish on topic`() {
        val message = TestDto(msg = "Hello")

        val app = EventApplication(emptyList(), "dev.bothin.smoothmqtt.publishers", mqttContainer.containerIpAddress, mqttContainer.getMappedPort(1883))
        val kodein = app.run()

        val testPublisher = kodein.direct.instance<TestPublisher>()

        GlobalScope.launch {
            delay(250)
            testPublisher.produce(message)
        }

        val listener = spyk({ _: String, _: MqttMessage ->

        })

        mqttClient.subscribe("event_publisher_test/topic_publish", listener)

        verify(exactly = 1, timeout = 1000) { listener("event_publisher_test/topic_publish", match { mapper.readValue(it.payload, TestDto::class.java) == message }) }
    }

}


@EventPublisher
internal interface TestPublisher {

    @EventProducer(topic = "event_publisher_test/topic_publish")
    fun produce(payload: TestDto)

}

internal data class TestDto(val msg: String)
