package dev.bothin.micromqtt.event

import com.fasterxml.jackson.databind.ObjectMapper
import dev.bothin.micromqtt.event.error.ExceptionHandler
import dev.bothin.micromqtt.mqtt.MicroMqttClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.nio.charset.StandardCharsets
import kotlin.system.measureTimeMillis

class EventClient(private val client: MicroMqttClient, private val mapper: ObjectMapper, private val exceptionHandler: ExceptionHandler) {

    fun eventSubscribe(topic: String, consumer: Consumer) {
        client.subscribe(topic, handleEvent(consumer, topic))
    }

    private fun handleEvent(consumer: Consumer, topic: String): (String, MqttMessage) -> Unit {
        return { t: String, message: MqttMessage ->
            val messageAsString = message.payload.toString(StandardCharsets.UTF_8)
            GlobalScope.launch {
                try {
                    val timeReflection = measureTimeMillis {
                        consume(consumer, topic, messageAsString)
                    }
                    println("Took $timeReflection ms to consume message on $t")
                } catch (e: Exception) {
                    println(e)
                    exceptionHandler.onException(e, t, message)
                }
            }
            Unit
        }
    }

    private fun consume(consumer: Consumer, topic: String, messageAsString: String) {
        val args = getArgs(consumer, messageAsString, topic)

        consumer.method.invoke(
            consumer.instance,
            *args.toTypedArray()
        )
    }

    private fun getArgs(consumer: Consumer, messageAsString: String, topic: String): List<Any> {
        return consumer.method.parameterAnnotations.mapNotNull {
            if (it.size == 1) {
                when (it.first().annotationClass) {
                    EventBody::class -> mapper.readValue(messageAsString, consumer.payloadType)
                    EventTopic::class -> topic
                    else -> null
                }
            } else {
                null
            }
        }
    }
}