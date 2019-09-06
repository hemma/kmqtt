package dev.bothin.kmqtt

import dev.bothin.kmqtt.mqtt.KMqttClient
import dev.bothin.kmqtt.mqtt.OnMessageType

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

