package dev.bothin.smoothmqtt.publisher

import dev.bothin.smoothmqtt.EventProducer
import dev.bothin.smoothmqtt.mqtt.SmoothMqttClient
import mu.KotlinLogging
import net.sf.cglib.proxy.MethodInterceptor
import net.sf.cglib.proxy.MethodProxy
import java.lang.reflect.Method

private val log = KotlinLogging.logger { }

class PublisherMethodInterceptor(private val smoothMqttClient: SmoothMqttClient) : MethodInterceptor {

    override fun intercept(obj: Any, method: Method, args: Array<out Any>, proxy: MethodProxy): Any? {
        try {
            if (method.isAnnotationPresent(EventProducer::class.java)) {
                val topic = method.getAnnotation(EventProducer::class.java).topic
                smoothMqttClient.emit(topic, args[0])
                return null
            }
        } catch (e: Exception) {
            log.error(e) { }
        }

        return null
    }
}