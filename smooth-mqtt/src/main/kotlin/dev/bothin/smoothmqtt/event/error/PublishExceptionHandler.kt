package dev.bothin.smoothmqtt.event.error

import dev.bothin.smoothmqtt.mqtt.SmoothMqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.nio.charset.Charset

class PublishExceptionHandler(private val smoothMqttClient: SmoothMqttClient) : ExceptionHandler {
    override fun onException(exception: Exception, topic: String, mqttMessage: MqttMessage) {
        val eventError = EventError(code = "Unknown", message = exception.message
            ?: "UnknownError", receivedEvent = mqttMessage.payload.toString(Charset.defaultCharset()))
        smoothMqttClient.emit("$topic/error", eventError)
    }
}