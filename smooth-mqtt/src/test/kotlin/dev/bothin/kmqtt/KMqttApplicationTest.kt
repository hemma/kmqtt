package dev.bothin.kmqtt

import dev.bothin.kmqtt.mqtt.InMessage
import dev.bothin.kmqtt.mqtt.KMqttClient
import dev.bothin.kmqtt.mqtt.OnMessageType
import dev.bothin.kmqtt.mqtt.OutMessage
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class KMqttApplicationTest {

    private lateinit var application: KMqttApplication

    @MockK
    private lateinit var mqttClient: KMqttClient

    @BeforeEach
    fun setUp() {
        // clearAllMocks()
        application = KMqttApplication(mqttClient)
    }

    @Test
    fun `given topic in when sub then subscribe to mqtt topic`() {
        val topicIn = "topic_in"

        every { mqttClient.subscribe(ofType(), null, ofType<OnMessageType<Msg>>(), ofType()) } just Runs

        application {
            subscribe(topicIn) { _: InMessage<Msg> ->
                OutMessage.nothing()
            }
        }

        verify(exactly = 1) { mqttClient.subscribe(topicIn, null, ofType<OnMessageType<Msg>>(), ofType()) }
    }

}

internal data class Msg(val hello: String)