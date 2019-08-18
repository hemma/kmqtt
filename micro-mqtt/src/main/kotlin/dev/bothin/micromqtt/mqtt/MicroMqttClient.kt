package dev.bothin.micromqtt.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage

class MicroMqttClient(private val mqttClient: MqttClient, private val mapper: ObjectMapper) : MqttCallback {

    init {
        mqttClient.setCallback(this)
        val options = MqttConnectOptions()
        options.maxInflight = 50
        options.isAutomaticReconnect = true
        mqttClient.connect(options)
    }

    fun <T : Any> emit(topic: String, payload: T, qos: Int = 1, retain: Boolean = false) {
        GlobalScope.launch(Dispatchers.IO) {
            var retry = false
            do {
                try {
                    mqttClient.publish(topic, mapper.writeValueAsBytes(payload), qos, retain)
                } catch (e: MqttException) {
                    if (e.reasonCode.toShort() == MqttException.REASON_CODE_MAX_INFLIGHT) {
                        retry = true
                    } else {
                        throw e
                    }
                }
            } while (retry)

        }
    }

    fun subscribe(topic: String, listener: (String, MqttMessage) -> Unit) {
        mqttClient.subscribe(topic, listener)
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {}

    override fun deliveryComplete(token: IMqttDeliveryToken?) {}

    override fun connectionLost(cause: Throwable?) {
        //mqttClient.connect()
    }
}