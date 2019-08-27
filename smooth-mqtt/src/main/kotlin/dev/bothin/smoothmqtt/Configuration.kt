package dev.bothin.smoothmqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bothin.smoothmqtt.mqtt.SmoothMqttClient
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import java.util.UUID

class Configuration {
    companion object {
        fun smoothMqttKodein(mqttHost: String, mqttPort: Int): Kodein.Module {
            return Kodein.Module("smooth-mqtt", allowSilentOverride = true) {
                bind<ObjectMapper>() with singleton {
                    val mapper = jacksonObjectMapper()
                    mapper.registerModule(KotlinModule())
                    mapper.registerModule(Jdk8Module())
                    mapper.registerModule(JavaTimeModule())
                    mapper
                }
                bind<MqttClient>() with singleton {
                    MqttClient(
                        "tcp://$mqttHost:$mqttPort",
                        "smooth_client_${UUID.randomUUID().toString().substring(0, 4)}",
                        MemoryPersistence()
                    )
                }
                bind<SmoothMqttClient>() with singleton { SmoothMqttClient(instance(), instance()) }
            }
        }
    }
}