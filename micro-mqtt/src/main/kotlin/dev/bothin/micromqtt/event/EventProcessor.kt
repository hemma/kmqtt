package dev.bothin.micromqtt.event

import org.kodein.di.Instance
import org.kodein.di.Kodein
import org.kodein.di.TT
import org.kodein.di.generic.instance
import org.reflections.Reflections

internal class EventProcessor(private val kodein: Kodein) {
    private val events = mutableMapOf<String, Event>()
    private val client by kodein.instance<EventClient>()

    fun setup() {
        findConsumers()
        subscribe()
    }

    private fun subscribe() {
        events.forEach { (topic, event) ->
            client.eventSubscribe(topic, event)
        }
    }

    private fun findConsumers() {
        val basePackageName = getBasePackageName()
        val reflections = Reflections(basePackageName)
        val annotated = reflections.getTypesAnnotatedWith(EventController::class.java)

        annotated.forEach {
            println(it.simpleName)
            val instance by kodein.Instance(TT(it))
            it.methods.filter { clazz -> clazz.isAnnotationPresent(EventConsumer::class.java) }
                .forEach { method ->
                    println("   ${method.name}")
                    var i = 0
                    val payloadTypeIndex = method.parameterAnnotations.mapNotNull { annotations ->
                        if (annotations.size == 1 && annotations.first().annotationClass == EventBody::class) {
                            i
                        } else {
                            i++
                            null
                        }
                    }
                    var consumePayloadType: Class<*>? = null
                    if (payloadTypeIndex.size == 1) {
                        consumePayloadType = method.parameters[payloadTypeIndex.first()].type
                    }

                    val consumeTopic = method.getAnnotation(EventConsumer::class.java).topic
                    val produceTopic = method.getAnnotation(EventProducer::class.java)?.topic ?: ""
                    val consumer = Consumer(topic = consumeTopic, payloadType = consumePayloadType)
                    val producer = Producer(topic = produceTopic)
                    val event = Event(instance = instance, method = method, consumer = consumer, producer = producer)
                    events[consumeTopic] = event
                }
        }
    }

    private fun getBasePackageName(): String {
        val stack = Thread.currentThread().stackTrace
        val main = stack[stack.size - 1]
        val mainClass = main.className
        val split = mainClass.split(".")
        return split.subList(0, split.size - 1).joinToString(".")
    }
}
