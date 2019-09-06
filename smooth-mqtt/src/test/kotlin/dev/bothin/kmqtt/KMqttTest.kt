package dev.bothin.kmqtt

import dev.bothin.kmqtt.mqtt.InMessage
import dev.bothin.kmqtt.mqtt.KMqttClient
import dev.bothin.kmqtt.mqtt.OnMessageType
import io.kotlintest.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.reflect.KClass

@ExtendWith(MockKExtension::class)
class KMqttTest {

    @MockK
    private lateinit var kMqttClient: KMqttClient

    private lateinit var kMqtt: KMqtt

    @BeforeEach
    fun setUp() {
        // clearAllMocks()
        kMqtt = KMqtt(kMqttClient)

        every { kMqttClient.unsubscribe(ofType()) } just Runs
    }

    @Test
    fun `when emit then emit`() {
        val topicOut = "out"
        val payload = Dto(msg = "msg")

        every { kMqttClient.emit(ofType(), ofType<Dto>()) } just Runs

        kMqtt.emit(topicOut, payload)

        verify(exactly = 1) { kMqttClient.emit(topicOut, payload) }
    }

    @Test
    fun `when wait receive then wait for message`() {
        val expectedResponse = InMessage("in", Dto(msg = "response"))
        val topicIn = "in"

        val onMessageSlot = slot<OnMessageType<Dto>>()
        every { kMqttClient.subscribe(ofType(), capture(onMessageSlot), ofType()) } just Runs

        GlobalScope.launch {
            delay(25)
            onMessageSlot.captured.invoke(expectedResponse)
        }
        val response = runBlocking {
            kMqtt.waitReceive<Dto>(topicIn)
        }

        response shouldBe expectedResponse.payload
    }

    @Test
    fun `when emit and wait receive then emit and wait for message`() {
        val expectedResponse = InMessage("in", Dto(msg = "response"))
        val topicIn = "in"
        val topicOut = "out"
        val payload = Dto(msg = "msg")

        val onMessageSlot = slot<OnMessageType<Dto>>()
        every { kMqttClient.emitReceive(ofType(), ofType<Dto>(), ofType(), ofType(), capture(onMessageSlot)) } just Runs

        GlobalScope.launch {
            delay(100)
            onMessageSlot.captured.invoke(expectedResponse)
        }
        val response = runBlocking {
            kMqtt.emitWaitReceive(topicOut, payload, topicIn)
        }

        verify(exactly = 1) { kMqttClient.emitReceive(topicOut, payload, topicIn, ofType<KClass<Dto>>(), ofType()) }
        response shouldBe expectedResponse.payload
    }
}

internal data class Dto(val msg: String)