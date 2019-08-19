package dev.bothin.smoothmqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bothin.smoothmqtt.event.EventClient
import dev.bothin.smoothmqtt.event.error.ExceptionHandler
import dev.bothin.smoothmqtt.event.error.PublishExceptionHandler
import dev.bothin.smoothmqtt.mqtt.SmoothMqttClient
import org.eclipse.paho.client.mqttv3.MqttClient
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

class Configuration {
    companion object {
        fun smoothMqttKodein(host: String, port: Int): Kodein.Module {
            return Kodein.Module("smooth-mqtt") {
                bind<ObjectMapper>() with singleton { jacksonObjectMapper() }
                bind<MqttClient>() with singleton { MqttClient("tcp://$host:$port", "smooth_mqtt_client") }
                bind<SmoothMqttClient>() with singleton { SmoothMqttClient(instance(), instance()) }
                bind<ExceptionHandler>() with singleton { PublishExceptionHandler(instance()) }
                bind<EventClient>() with singleton { EventClient(instance(), instance(), instance()) }
            }
        }
    }
}