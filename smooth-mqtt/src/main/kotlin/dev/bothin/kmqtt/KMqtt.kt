package dev.bothin.kmqtt

import dev.bothin.kmqtt.mqtt.InMessage
import dev.bothin.kmqtt.mqtt.KMqttClient
import dev.bothin.kmqtt.mqtt.OnMessageType
import dev.bothin.kmqtt.mqtt.OutMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.time.Duration

class KMqtt(val kMqttClient: KMqttClient) {

    fun <T : Any> emit(topicOut: String, payload: T, qos: Int = 1, retain: Boolean = false) {
        return kMqttClient.emit(topicOut, payload, qos, retain)
    }

    suspend inline fun <reified T : Any> waitReceive(topicIn: String,
                                                     timeout: Duration = Duration.ofSeconds(30)): T {
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
                                                         timeout: Duration = Duration.ofSeconds(30),
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

fun main(args: Array<String>) {
    val mqttClient = MqttClient("tcp://localhost:1883", "clientID", MemoryPersistence())
    val client = KMqttClient(mqttClient)
    client.connect()
    val kMqtt = KMqtt(client)

    val msg = runBlocking {
        try {
            kMqtt.emitWaitReceive("out", Msg2("out"), "in")
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    println(msg)

    GlobalScope.launch {
        delay(250)
        kMqtt.emit("out2", Msg2("yolo"))
    }

    val msg2 = runBlocking {
        kMqtt.waitReceive<Msg2>("out2")
    }

    println(msg2)
    println("DONE")

    client.unsubscribeAll()
    client.disconnect()
}

internal data class Msg2(val name: String)