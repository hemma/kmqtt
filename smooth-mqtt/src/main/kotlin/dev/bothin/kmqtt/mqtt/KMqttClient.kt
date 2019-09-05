package dev.bothin.kmqtt.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.nio.charset.Charset
import kotlin.reflect.KClass

typealias OnMessageMqtt = (String, MqttMessage) -> Unit
typealias OnMessageType <T> = (InMessage<T>) -> OutMessage<Any>?

data class InMessage<T : Any>(val topic: String, val payload: T)
data class OutMessage<T : Any>(val payload: T?) {
    companion object {
        fun nothing() = OutMessage<Any>(null)
    }
}

class KMqttClient(private val mqttClient: MqttClient, private val objectMapper: ObjectMapper = jacksonObjectMapper(),
                  private val mqttOptions: MqttOptions = MqttOptions.default()) : MqttCallback {

    private val onMessageBlocks = mutableMapOf<String, OnMessageMqtt>()

    fun connect() {
        mqttClient.setCallback(this)
        val options = MqttConnectOptions()
        options.maxInflight = mqttOptions.maxInFlight
        options.isAutomaticReconnect = mqttOptions.automaticReconnect
        if (mqttOptions.username != null && mqttOptions.password != null) {
            options.userName = mqttOptions.username
            options.password = mqttOptions.password.toCharArray()
        }
        options.keepAliveInterval = mqttOptions.keepAliveInterval
        options.connectionTimeout = mqttOptions.connectionTimeOut
        mqttClient.connect(options)
    }

    fun <T : Any, R : Any> emitReceive(topicOut: String,
                                       payload: T,
                                       topicIn: String,
                                       responseType: KClass<R>,
                                       block: OnMessageType<R>,
                                       qos: Int = 1,
                                       retain: Boolean = false) {
        emit(topicOut, payload, qos, retain)
        subscribe(topicIn, block, responseType)
    }

    fun <T : Any> emit(topicOut: String, payload: T, qos: Int = 1, retain: Boolean = false) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                mqttClient.publish(topicOut, objectMapper.writeValueAsBytes(payload), qos, retain)
            } catch (e: MqttException) {
                if (e.reasonCode.toShort() == MqttException.REASON_CODE_MAX_INFLIGHT ||
                    e.reasonCode.toShort() == MqttException.REASON_CODE_CLIENT_NOT_CONNECTED ||
                    e.reasonCode.toShort() == MqttException.REASON_CODE_CONNECTION_LOST) {
                    throw RuntimeException(e)
                } else {
                    throw RuntimeException(e)
                }
            }
        }
    }

    fun <T : Any> subscribe(topicIn: String, block: OnMessageType<T>, type: KClass<T>) {
        subscribe(topicIn, null, block, type)
    }

    fun <T : Any> subscribe(topicIn: String, topicOut: String? = null, block: OnMessageType<T>, type: KClass<T>) {
        mqttClient.subscribe(topicIn)
        if (isRegisteredTopic(topicIn)) {
            throw RuntimeException("Only one subscription per topicIn, $topicIn")
        }
        onMessageBlocks[topicIn] = onMessage(block, type, topicOut)
    }

    fun unsubscribe(topic: String) {
        mqttClient.unsubscribe(topic)
        onMessageBlocks.remove(topic)
    }

    private fun <T : Any> onMessage(block: OnMessageType<T>, type: KClass<T>, topicOut: String?): OnMessageMqtt {
        return { topic: String, message: MqttMessage ->
            val payload = map(message, type)
            block.invoke(InMessage(topic, payload))?.let {
                if (topicOut != null && it.payload != null) {
                    emit(topicOut, it.payload)
                }
            } ?: Unit
        }
    }

    private fun isRegisteredTopic(topic: String): Boolean {
        return topic in onMessageBlocks
    }

    private fun <T : Any> map(message: MqttMessage, type: KClass<T>): T {
        return objectMapper.readValue(message.payload.toString(Charset.defaultCharset()), type.java)
    }

    override fun messageArrived(topic: String, message: MqttMessage) {
        GlobalScope.launch(Dispatchers.IO) {
            onMessageBlocks.filterKeys { key ->
                topic.startsWith(key.replace("#", "").replace("+", ""))
            }.map {
                it.value.invoke(topic, message)
            }
        }
    }

    override fun connectionLost(cause: Throwable?) {
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
    }

    fun unsubscribeAll() {
        onMessageBlocks.keys.forEach {
            mqttClient.unsubscribe(it)
        }
        onMessageBlocks.clear()
    }

    fun disconnect() {
        mqttClient.disconnect()
    }

}


fun main(args: Array<String>) {
    val mqttClient = MqttClient("tcp://localhost:1883", "clientID", MemoryPersistence())
    val client = KMqttClient(mqttClient)
    client.connect()
    val onMessageType: OnMessageType<Msg> = { inMsg: InMessage<Msg> ->
        println("Message: ${inMsg.payload}")
        OutMessage(inMsg.payload.copy(name = "new_name"))
    }
    client.subscribe(topicIn = "hello/2", block = onMessageType, type = Msg::class)
    client.subscribe(topicIn = "hello", topicOut = "bye", block = onMessageType, type = Msg::class)

    while (true) {

    }
}

internal data class Msg(val name: String)