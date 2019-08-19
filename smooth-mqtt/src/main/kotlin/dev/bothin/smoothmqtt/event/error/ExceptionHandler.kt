package dev.bothin.smoothmqtt.event.error

import org.eclipse.paho.client.mqttv3.MqttMessage

interface ExceptionHandler {
    fun onException(exception: Exception, topic: String, mqttMessage: MqttMessage)
}