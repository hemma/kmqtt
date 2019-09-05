package dev.bothin.kmqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bothin.kmqtt.mqtt.InMessage
import dev.bothin.kmqtt.mqtt.KMqttClient
import dev.bothin.kmqtt.mqtt.OnMessageType
import dev.bothin.kmqtt.mqtt.OutMessage
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class KMqttApplication(val kMqttClient: KMqttClient) {

    inline fun <reified T : Any> subscribe(topicIn: String, noinline block: OnMessageType<T>) {
        kMqttClient.subscribe(topicIn = topicIn, topicOut = null, block = block, type = T::class)
    }

    inline fun <reified T : Any> subscribe(topicIn: String, topicOut: String, noinline block: OnMessageType<T>) {
        kMqttClient.subscribe(topicIn = topicIn, topicOut = topicOut, block = block, type = T::class)
    }

    operator fun invoke(block: KMqttApplication.() -> Unit) {
        block()
    }

    fun transactional(block: KMqttApplication.() -> Unit) {
        println("transaction start")
        block()
        println("transaction end")
    }

}

fun run() {
    val mqttClient = MqttClient("tcp://localhost:1883", "clientID", MemoryPersistence())
    val kMqttClient = KMqttClient(mqttClient, jacksonObjectMapper().findAndRegisterModules())
    kMqttClient.connect()
    val app = KMqttApplication(kMqttClient)

    app {

        transactional {
            subscribe("hello") { msg: InMessage<Dto> ->
                // do something
                println("hello: ${msg.payload.msg}")
                OutMessage.nothing()
            }
        }

        subscribe("hello/r", "bye") { msg: InMessage<Dto> ->
            // do something
            OutMessage(payload = msg.payload.copy(msg = "new_message"))
        }

    }
}

internal data class Dto(val msg: String)

fun main(args: Array<String>) {
    run()
}

