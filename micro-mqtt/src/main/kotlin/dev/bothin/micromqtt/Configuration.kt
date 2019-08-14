package dev.bothin.micromqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bothin.micromqtt.event.EventClient
import dev.bothin.micromqtt.event.error.ExceptionHandler
import dev.bothin.micromqtt.event.error.PublishExceptionHandler
import dev.bothin.micromqtt.mqtt.MicroMqttClient
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

class Configuration {
    companion object {
        val microMqttKodein = Kodein.Module("micro-mqtt") {
            bind<ObjectMapper>() with singleton { jacksonObjectMapper() }
            bind<MicroMqttClient>() with singleton { MicroMqttClient(instance()) }
            bind<ExceptionHandler>() with singleton { PublishExceptionHandler(instance()) }
            bind<EventClient>() with singleton { EventClient(instance(), instance(), instance()) }
        }
    }
}