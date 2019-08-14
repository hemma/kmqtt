package dev.bothin.micromqtt.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

open class MicroMqttClient : MqttCallback {

    private val client: MqttClient

    private val mapper: ObjectMapper

    constructor(mapper: ObjectMapper) {
        this.mapper = mapper
        client = MqttClient("tcp://localhost:1883", "clientID")
        client.setCallback(this)
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        client.connect(options)
    }

    fun <T : Any> emit(topic: String, payload: T, qos: Int = 1, retain: Boolean = false) {
        GlobalScope.launch {
            client.publish(topic, mapper.writeValueAsBytes(payload), qos, retain)
        }
    }

    fun subscribe(topic: String, listener: (String, MqttMessage) -> Unit) {
        client.subscribe(topic, listener)
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {}

    override fun deliveryComplete(token: IMqttDeliveryToken?) {}

    override fun connectionLost(cause: Throwable?) {
        client.connect()
    }
}