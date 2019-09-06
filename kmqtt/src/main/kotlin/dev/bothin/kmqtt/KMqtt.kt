package dev.bothin.kmqtt

import dev.bothin.kmqtt.mqtt.InMessage
import dev.bothin.kmqtt.mqtt.KMqttClient
import dev.bothin.kmqtt.mqtt.OnMessageType
import dev.bothin.kmqtt.mqtt.OutMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.Duration

const val DEFAULT_TIMEOUT: Long = 15

class KMqtt(val kMqttClient: KMqttClient) {

    fun <T : Any> emit(topicOut: String, payload: T, qos: Int = 1, retain: Boolean = false) {
        return kMqttClient.emit(topicOut, payload, qos, retain)
    }

    suspend inline fun <reified T : Any> waitReceive(topicIn: String,
                                                     timeout: Duration = Duration.ofSeconds(DEFAULT_TIMEOUT)): T {
        return withTimeout(timeout.toMillis()) {
            val msgChannel = Channel<InMessage<T>>()
            val onMessage: OnMessageType<T> = {
                GlobalScope.launch {
                    msgChannel.send(it)
                }
                OutMessage.nothing()
            }
            kMqttClient.subscribe(topicIn, onMessage, T::class)
            val response = msgChannel.receive().payload
            msgChannel.close()
            kMqttClient.unsubscribe(topicIn)
            response
        }

    }


    suspend inline fun <reified T : Any> emitWaitReceive(topicOut: String, payload: T,
                                                         topicIn: String,
                                                         timeout: Duration = Duration.ofSeconds(DEFAULT_TIMEOUT),
                                                         qos: Int = 1,
                                                         retain: Boolean = false): T {
        return withTimeout(timeout.toMillis()) {
            val msgChannel = Channel<InMessage<T>>()
            val onMessage: OnMessageType<T> = {
                GlobalScope.launch {
                    msgChannel.send(it)
                }
                OutMessage.nothing()
            }
            kMqttClient.emitReceive(topicOut, payload, topicIn, T::class, onMessage, qos, retain)
            val response = msgChannel.receive()
            kMqttClient.unsubscribe(topicIn)
            msgChannel.close()
            response.payload
        }
    }
}