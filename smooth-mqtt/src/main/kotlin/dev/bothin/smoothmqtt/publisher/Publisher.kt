package dev.bothin.smoothmqtt.publisher

import dev.bothin.smoothmqtt.mqtt.SmoothMqttClient
import net.sf.cglib.proxy.Enhancer
import org.kodein.di.Kodein
import org.kodein.di.TT
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton


class Publisher(private val instanceType: Class<*>, private val methods: List<OnPublish>) {

    fun proxy(smoothMqttClient: SmoothMqttClient): Any {
        val enhancer = Enhancer()
        enhancer.setSuperclass(instanceType)
        enhancer.setCallback(PublisherMethodInterceptor(smoothMqttClient))
        return enhancer.create()
    }

    fun proxy(): Kodein.Module {
        return Kodein.Module(instanceType.name) {
            Bind(TT(instanceType)) with singleton {
                val enhancer = Enhancer()
                enhancer.setSuperclass(instanceType)
                enhancer.setCallback(PublisherMethodInterceptor(instance()))
                enhancer.create()
            }
        }
    }
}