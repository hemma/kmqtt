package dev.bothin.smoothmqtt.publisher

import dev.bothin.smoothmqtt.EventProducer
import dev.bothin.smoothmqtt.EventPublisher
import org.reflections.Reflections

class SmoothPublisher {
    companion object {
        fun findPublishers(packageName: String): List<Publisher> {
            val reflections = Reflections(packageName)
            val annotated = reflections.getTypesAnnotatedWith(EventPublisher::class.java)

            return annotated.map {
                val methods = it.methods.filter { clazz -> clazz.isAnnotationPresent(EventProducer::class.java) }
                    .map { method ->
                        val topic = method.getAnnotation(EventProducer::class.java).topic
                        OnPublish(topic, method)
                    }
                Publisher(it, methods)
            }
        }
    }
}