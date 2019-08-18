package dev.bothin.micromqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bothin.micromqtt.event.EventClient
import dev.bothin.micromqtt.event.error.ExceptionHandler
import dev.bothin.micromqtt.event.error.PublishExceptionHandler
import dev.bothin.micromqtt.mqtt.MicroMqttClient
import org.eclipse.paho.client.mqttv3.MqttClient
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

class Configuration {
    companion object {
        fun microMqttKodein(host: String, port: Int): Kodein.Module {
            return Kodein.Module("micro-mqtt") {
                bind<ObjectMapper>() with singleton { jacksonObjectMapper() }
                bind<MqttClient>() with singleton { MqttClient("tcp://$host:$port", "micro_mqtt_client") }
                bind<MicroMqttClient>() with singleton { MicroMqttClient(instance(), instance()) }
                bind<ExceptionHandler>() with singleton { PublishExceptionHandler(instance()) }
                bind<EventClient>() with singleton { EventClient(instance(), instance(), instance()) }
            }
        }
    }
}