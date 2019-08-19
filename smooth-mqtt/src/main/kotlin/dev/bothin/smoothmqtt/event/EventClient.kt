package dev.bothin.smoothmqtt.event

import com.fasterxml.jackson.databind.ObjectMapper
import dev.bothin.smoothmqtt.event.error.ExceptionHandler
import dev.bothin.smoothmqtt.mqtt.SmoothMqttClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.nio.charset.StandardCharsets
import kotlin.system.measureTimeMillis

class EventClient(private val client: SmoothMqttClient, private val mapper: ObjectMapper, private val exceptionHandler: ExceptionHandler) {

    fun eventSubscribe(controller: Controller) {
        client.subscribe(controller.consumer.topic, handleEvent(controller))
    }

    private fun handleEvent(controller: Controller): (String, MqttMessage) -> Unit {
        return { topic: String, message: MqttMessage ->
            val messageAsString = message.payload.toString(StandardCharsets.UTF_8)
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val timeReflection = measureTimeMillis {
                        val producePayload = consume(controller, topic, messageAsString)
                        if (producePayload != null && controller.producer.topic.isNotEmpty()) {
                            produce(controller, producePayload)
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

    private fun consume(controller: Controller, topic: String, messageAsString: String): Any? {
        val args = getArgs(controller, messageAsString, topic).toTypedArray()
        return controller.method.invoke(
            controller.instance,
            *args
        )
    }

    private fun produce(controller: Controller, payload: Any) {
        client.emit(controller.producer.topic, payload)
    }

    private fun getArgs(controller: Controller, messageAsString: String, topic: String): List<Any> {
        return controller.method.parameterAnnotations.mapNotNull {
            if (it.size == 1) {
                when (it.first().annotationClass) {
                    EventBody::class -> mapper.readValue(messageAsString, controller.consumer.payloadType)
                    EventTopic::class -> topic
                    else -> null
                }
            } else {
                null
            }
        }
    }
}