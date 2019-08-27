package dev.bothin.smoothmqtt.controller

import com.fasterxml.jackson.databind.ObjectMapper
import dev.bothin.smoothmqtt.error.ErrorAdvice
import dev.bothin.smoothmqtt.mqtt.SmoothMqttClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.kodein.di.Instance
import org.kodein.di.Kodein
import org.kodein.di.TT
import org.kodein.di.direct
import org.kodein.di.generic.instance
import org.kodein.di.generic.instanceOrNull
import java.nio.charset.StandardCharsets


class Controller(private val instanceType: Class<*>, private val methods: List<OnEvent>) {

    fun register(kodein: Kodein) {
        val instance by kodein.Instance(TT(instanceType))
        register(kodein.direct.instance(), instance, kodein.direct.instance(), kodein.direct.instanceOrNull())
    }

    fun register(smoothMqttClient: SmoothMqttClient, instance: Any, mapper: ObjectMapper, errorAdvice: ErrorAdvice? = null) {
        if (instance.javaClass != instanceType) {
            throw RuntimeException()
        }

        methods.forEach {
            smoothMqttClient.subscribe(it.topic) { incomingTopic: String, message: MqttMessage ->
                GlobalScope.launch {
                    try {
                        val messageAsString = message.payload.toString(StandardCharsets.UTF_8)
                        val payload = it.payload?.let { p ->
                            mapper.readValue(messageAsString, p.instanceType)
                        }

                        val args = getArgs(it, payload, incomingTopic)
                        val producePayload = it.method.invoke(instance, *args)
                        producePayload?.let { p ->
                            smoothMqttClient.emit(it.produceTopic, p)
                        }
                    } catch (e: Exception) {
                        errorAdvice?.onException(e, it.topic, incomingTopic, message)
                    }
                }
            }
        }
    }

    private fun getArgs(it: OnEvent, payload: Any?, incomingTopic: String): Array<Any> {
        return when {
            it.method.parameterCount == 1 && payload != null -> arrayOf(payload)
            it.method.parameterCount == 1 && payload == null -> arrayOf(incomingTopic)
            payload != null && it.method.parameterCount == 2 ->
                when {
                    it.payload!!.argumentIndex == 0 -> arrayOf(payload, incomingTopic)
                    else -> arrayOf(incomingTopic, payload)
                }
            else -> emptyArray()
        }
    }
}