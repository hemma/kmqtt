package dev.bothin.smoothmqtt.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage

class SmoothMqttClient(private val mqttClient: MqttClient, private val mapper: ObjectMapper) : MqttCallback {

    init {
        mqttClient.setCallback(this)
        val options = MqttConnectOptions()
        options.keepAliveInterval = 5
        options.maxReconnectDelay = 10
        options.isCleanSession = true
        options.maxInflight = 50
        options.isAutomaticReconnect = true
        mqttClient.connect(options)
    }

    fun <T : Any> emit(topic: String, payload: T, qos: Int = 1, retain: Boolean = false) {
        GlobalScope.launch(Dispatchers.IO) {
            var retry = false
            var retries = 0
            do {
                try {
                    mqttClient.publish(topic, mapper.writeValueAsBytes(payload), qos, retain)
                } catch (e: MqttException) {
                    if (e.reasonCode.toShort() == MqttException.REASON_CODE_MAX_INFLIGHT || e.reasonCode.toShort() == MqttException.REASON_CODE_CLIENT_NOT_CONNECTED) {
                        retry = true
                        retries++
                        delay(1000 * retries.toLong())
                    } else {
                        throw e
                    }
                }
            } while (retry && retries < 5)

        }
    }

    fun subscribe(topic: String, listener: (String, MqttMessage) -> Unit) {
        mqttClient.subscribe(topic, listener)
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {}

    override fun deliveryComplete(token: IMqttDeliveryToken?) {}

    override fun connectionLost(cause: Throwable?) {
        mqttClient.connect()
    }
}