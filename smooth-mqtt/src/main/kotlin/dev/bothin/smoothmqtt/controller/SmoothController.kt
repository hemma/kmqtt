package dev.bothin.smoothmqtt.controller

import dev.bothin.smoothmqtt.EventBody
import dev.bothin.smoothmqtt.EventConsumer
import dev.bothin.smoothmqtt.EventController
import dev.bothin.smoothmqtt.EventProducer
import org.reflections.Reflections

class SmoothController {
    companion object {
        fun findControllers(packageName: String): List<Controller> {
            val reflections = Reflections(packageName)
            val annotated = reflections.getTypesAnnotatedWith(EventController::class.java)

            return annotated.map {
                val methods = it.methods.filter { clazz -> clazz.isAnnotationPresent(EventConsumer::class.java) }.map { method ->
                    val topic = method.getAnnotation(EventConsumer::class.java).topic
                    val produceTopic = method.getAnnotation(EventProducer::class.java)?.topic ?: ""
                    val payload = method.parameters.mapNotNull { parameter ->
                        parameter.annotations.firstOrNull { annotation ->
                            annotation.annotationClass == EventBody::class
                        }?.let {
                            val index = method.parameters.indexOf(parameter)
                            val instanceType = parameter.type
                            Payload(instanceType, index)
                        }
                    }.firstOrNull()

                    OnEvent(topic, produceTopic, method, payload)
                }
                Controller(it, methods)
            }
        }
    }
}
