package dev.bothin.smoothmqtt.examples

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bothin.smoothmqtt.Configuration
import dev.bothin.smoothmqtt.SmoothProcessor
import dev.bothin.smoothmqtt.mqtt.SmoothMqttClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

fun main(args: Array<String>) {
    //legacy()
    //withoutKodein()
    withKodein()
}

private fun withKodein() {
    val controllers = SmoothProcessor.findControllers("dev.bothin.smoothmqtt.examples")
    val publishers = SmoothProcessor.findPublishers("dev.bothin.smoothmqtt.examples").map { it.proxy() }

    val kodein = Kodein {
        importAll(publishers)
        import(Configuration.smoothMqttKodein("localhost", 1883))
        bind<HelloController>() with singleton { HelloController() }
    }
    controllers.forEach {
        it.register(kodein)
    }

    val helloPublisher = kodein.direct.instance<HelloPublisher>()
    GlobalScope.launch {
        delay(1000)
        helloPublisher.byeWorld(NameDto("hb"))
    }
}

private fun withoutKodein() {
    val mqttClient = MqttClient("tcp://localhost:1883", "clientID", MemoryPersistence())
    val smoothMqttClient = SmoothMqttClient(mqttClient, jacksonObjectMapper())
    val controllers = SmoothProcessor.findControllers("dev.bothin.smoothmqtt.examples")
    val publishers = SmoothProcessor.findPublishers("dev.bothin.smoothmqtt.examples")

    controllers.forEach { it.register(smoothMqttClient, HelloController(), jacksonObjectMapper()) }
    val proxies = publishers.map { it.proxy(smoothMqttClient) }
    GlobalScope.launch {
        delay(1000)
        (proxies.first() as HelloPublisher).byeWorld(NameDto("byebye"))
    }
}