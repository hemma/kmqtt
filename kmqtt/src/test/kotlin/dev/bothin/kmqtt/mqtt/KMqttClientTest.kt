package dev.bothin.kmqtt.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.charset.Charset

@ExtendWith(MockKExtension::class)
class KMqttClientTest {

    @MockK
    private lateinit var mqttClient: MqttClient

    private lateinit var kMqttClient: KMqttClient

    @BeforeEach
    fun setUp() {

    }

    @Nested
    inner class Connect {

        @BeforeEach
        fun setUp() {
            kMqttClient = KMqttClient(mqttClient)
        }

        @Test
        fun `when connect then call mqtt client with connect options`() {
            every { mqttClient.connect(ofType()) } just Runs
            every { mqttClient.setCallback(ofType()) } just Runs

            kMqttClient.connect()

            verify(exactly = 1) { mqttClient.connect(ofType()) }
        }

        @Test
        fun `when connect set callback to this`() {
            every { mqttClient.connect(ofType()) } just Runs
            every { mqttClient.setCallback(ofType()) } just Runs

            kMqttClient.connect()

            verify(exactly = 1) { mqttClient.setCallback(kMqttClient) }
        }
    }

    @Nested
    inner class Emit {

        private val mapper = jacksonObjectMapper()

        @BeforeEach
        fun setUp() {
            kMqttClient = KMqttClient(mqttClient)
        }

        @Test
        fun `when emit then publish to mqtt`() {
            every { mqttClient.publish(ofType(), ofType(), ofType(), ofType()) } just Runs

            val topicOut = "out"
            val payload = TDto("msg")
            kMqttClient.emit(topicOut, payload.copy())

            verify(exactly = 1, timeout = 250) {
                mqttClient.publish(topicOut, match {
                    mapper.readValue<TDto>(it.toString(Charset.defaultCharset())) == payload
                }, ofType(), ofType())
            }
        }

    }

    @Nested
    inner class Subscribe {

        @BeforeEach
        fun setUp() {
            kMqttClient = KMqttClient(mqttClient)
        }

        @Test
        fun `when subscribe then subscribe to mqtt`() {
            every { mqttClient.subscribe(ofType<String>()) } just Runs
            val topic = "topic"

            kMqttClient.subscribe(topic, { OutMessage.nothing() }, TDto::class)

            verify(exactly = 1) { mqttClient.subscribe(topic) }
        }
    }

    // unsubscribe
    @Nested
    inner class Unsubscribe {

        @BeforeEach
        fun setUp() {
            kMqttClient = KMqttClient(mqttClient)
        }

        @Test
        fun `when unsubscribe then unsubscribe to mqtt`() {
            every { mqttClient.unsubscribe(ofType<String>()) } just Runs
            val topic = "topic"

            kMqttClient.unsubscribe(topic)

            verify(exactly = 1) { mqttClient.unsubscribe(topic) }
        }
    }

    @Nested
    inner class MessageArrived {

        private val mapper = jacksonObjectMapper()

        @BeforeEach
        fun setUp() {
            kMqttClient = KMqttClient(mqttClient)
            every { mqttClient.subscribe(ofType<String>()) } just Runs
        }

        @Test
        fun `given wildcard subscription when matching message then call subscriber`() {
            val subscriptionTopic = "#"

            val onMessageMock = mockk<OnMessageType<TDto>>(relaxed = true)

            kMqttClient.subscribe(subscriptionTopic, onMessageMock, TDto::class)

            val dto = TDto("msg")
            val topic = "topic/topic/hello"
            kMqttClient.messageArrived(topic, MqttMessage(mapper.writeValueAsBytes(dto)))

            verify(exactly = 1, timeout = 250) {
                onMessageMock(match {
                    it.payload == dto && it.topic == topic
                })
            }
        }

        @Test
        fun `given plus wildcard subscription when matching message then call subscriber`() {
            val subscriptionTopic = "topic/+"

            val onMessageMock = mockk<OnMessageType<TDto>>(relaxed = true)

            kMqttClient.subscribe(subscriptionTopic, onMessageMock, TDto::class)

            val dto = TDto("msg")
            val topic = "topic/1"
            kMqttClient.messageArrived(topic, MqttMessage(mapper.writeValueAsBytes(dto)))

            verify(exactly = 1, timeout = 250) {
                onMessageMock(match {
                    it.payload == dto
                })
            }
        }

        @Test
        fun `given plus wildcard subscription when not matching message then do not call subscriber`() {
            val subscriptionTopic = "topic/+"

            val onMessageMock = mockk<OnMessageType<TDto>>(relaxed = true)

            kMqttClient.subscribe(subscriptionTopic, onMessageMock, TDto::class)

            val dto = TDto("msg")
            val topic = "topic/1/1"
            kMqttClient.messageArrived(topic, MqttMessage(mapper.writeValueAsBytes(dto)))

            verify(exactly = 0, timeout = 250) {
                onMessageMock(ofType())
            }
        }

        @Test
        fun `given subscription when matching message then call subscriber`() {
            val subscriptionTopic = "topic"

            val onMessageMock = mockk<OnMessageType<TDto>>(relaxed = true)

            kMqttClient.subscribe(subscriptionTopic, onMessageMock, TDto::class)

            val dto = TDto("msg")
            val topic = "topic"
            kMqttClient.messageArrived(topic, MqttMessage(mapper.writeValueAsBytes(dto)))

            verify(exactly = 1, timeout = 250) {
                onMessageMock(match {
                    it.payload == dto
                })
            }
        }


        @Test
        fun `given subscription when not matching message then do not call subscriber`() {
            val subscriptionTopic = "topic"

            val onMessageMock = mockk<OnMessageType<TDto>>(relaxed = true)

            kMqttClient.subscribe(subscriptionTopic, onMessageMock, TDto::class)

            val dto = TDto("msg")
            val topic = "wrong"
            kMqttClient.messageArrived(topic, MqttMessage(mapper.writeValueAsBytes(dto)))

            verify(exactly = 0, timeout = 250) {
                onMessageMock(ofType())
            }
        }
    }

}

internal data class TDto(val msg: String)