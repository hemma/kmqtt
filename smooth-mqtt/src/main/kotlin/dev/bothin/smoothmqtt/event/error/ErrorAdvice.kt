package dev.bothin.smoothmqtt.event.error

import org.eclipse.paho.client.mqttv3.MqttMessage

interface ErrorAdvice {

    fun onException(exception: Exception, consumingTopic: String, incomingTopic: String, mqttMessage: MqttMessage)

}