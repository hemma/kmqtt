package dev.bothin.micromqtt.event.error

import dev.bothin.micromqtt.mqtt.MicroMqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.nio.charset.Charset

class PublishExceptionHandler(private val microMqttClient: MicroMqttClient) : ExceptionHandler {
    override fun onException(exception: Exception, topic: String, mqttMessage: MqttMessage) {
        val eventError = EventError(code = "Unknown", message = exception.message
            ?: "UnknownError", receivedEvent = mqttMessage.payload.toString(Charset.defaultCharset()))
        microMqttClient.emit("$topic/error", eventError)
    }
}