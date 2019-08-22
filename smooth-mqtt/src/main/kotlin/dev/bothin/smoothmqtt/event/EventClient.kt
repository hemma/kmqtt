package dev.bothin.smoothmqtt.event

import com.fasterxml.jackson.databind.ObjectMapper
import dev.bothin.smoothmqtt.event.error.ErrorAdvice
import dev.bothin.smoothmqtt.mqtt.SmoothMqttClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.nio.charset.StandardCharsets

private val log = KotlinLogging.logger { }

class EventClient(
        private val client: SmoothMqttClient,
        private val mapper: ObjectMapper,
        private val errorAdvice: ErrorAdvice?
) {

    fun eventSubscribe(controller: Controller) {
        client.subscribe(controller.consumer.topic, handleEvent(controller))
    }

    private fun handleEvent(controller: Controller): (String, MqttMessage) -> Unit {
        return { topic: String, message: MqttMessage ->
            try {
                val messageAsString = message.payload.toString(StandardCharsets.UTF_8)
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val producePayload = consume(controller, topic, messageAsString)
                        if (producePayload != null && controller.producer.topic.isNotEmpty()) {
                            produce(controller, producePayload)
                        }
                    } catch (e: Exception) {
                        log.error(e) { "Failed to handle event on topic $topic" }
                        errorAdvice?.onException(e, controller.consumer.topic, topic, message)
                    }
                }
            } catch (e: Exception) {
                log.error(e) { "Failed to handle event on topic $topic" }
                errorAdvice?.onException(e, controller.consumer.topic, topic, message)
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