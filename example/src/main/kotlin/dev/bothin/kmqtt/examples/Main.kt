package dev.bothin.kmqtt.examples

import dev.bothin.kmqtt.KMqtt
import dev.bothin.kmqtt.KMqttApplication
import dev.bothin.kmqtt.mqtt.KMqttClient
import dev.bothin.kmqtt.mqtt.OutMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

fun main(args: Array<String>) {
    val mqttClient = MqttClient("tcp://localhost:1883", "clientId", MemoryPersistence())
    val kMqttClient = KMqttClient(mqttClient)
    kMqttClient.connect()

    val kMqtt = KMqtt(kMqttClient)

    val app = KMqttApplication(kMqttClient)
    app {
        subscribe<Person>("hello/1") {
            println("Hello ${it.payload.name}")
            OutMessage.nothing()
        }

        subscribe<Person>("hello/+", "hello/response") {
            println("Hello + ${it.payload.name}")
            OutMessage(payload = it.payload.copy(name = "Kmqtt"), topicSuffix = "id")
        }

        subscribe<Any>("#") {
            println("Log: ${it.payload} on ${it.topic}")
            OutMessage.nothing()
        }
    }

    kMqtt.emit("hello/1", Person("Stefan"))

    runBlocking {
        println("WaitReceive ${kMqtt.waitReceive<Person>("hello/response/+")}")

        println("EmitWaitReceive: ${kMqtt.emitWaitReceive("self", Person("Who?"), "self")}")
    }


    runBlocking {
        delay(1000)
        kMqttClient.unsubscribeAll()
        kMqttClient.disconnect()
    }
}

data class Person(val name: String)
