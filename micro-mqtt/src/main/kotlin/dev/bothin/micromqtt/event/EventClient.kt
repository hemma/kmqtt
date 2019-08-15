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

    fun eventSubscribe(topic: String, event: Event) {
        client.subscribe(topic, handleEvent(event))
    }

    private fun handleEvent(event: Event): (String, MqttMessage) -> Unit {
        return { topic: String, message: MqttMessage ->
            val messageAsString = message.payload.toString(StandardCharsets.UTF_8)
            GlobalScope.launch {
                try {
                    val timeReflection = measureTimeMillis {
                        val producePayload = consume(event, topic, messageAsString)
                        if (event.producer.topic.isNotEmpty() && producePayload != null) {
                            produce(event, producePayload)
                        }
                    }
                    println("Took $timeReflection ms to consume message on $topic")
                } catch (e: Exception) {
                    println(e)
                    exceptionHandler.onException(e, topic, message)
                }
            }
            Unit
        }
    }

    private fun produce(event: Event, payload: Any) {
        client.emit(event.producer.topic, payload)
    }

    private fun consume(event: Event, topic: String, messageAsString: String): Any? {
        val args = getArgs(event, messageAsString, topic)

        return event.method.invoke(
            event.instance,
            *args.toTypedArray()
        )
    }

    private fun getArgs(event: Event, messageAsString: String, topic: String): List<Any> {
        return event.method.parameterAnnotations.mapNotNull {
            if (it.size == 1) {
                when (it.first().annotationClass) {
                    EventBody::class -> mapper.readValue(messageAsString, event.consumer.payloadType)
                    EventTopic::class -> topic
                    else -> null
                }
            } else {
                null
            }
        }
    }
}