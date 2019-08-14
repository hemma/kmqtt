package dev.bothin.micromqtt.event

import org.kodein.di.Instance
import org.kodein.di.Kodein
import org.kodein.di.TT
import org.kodein.di.generic.instance
import org.reflections.Reflections

internal class EventProcessor(private val kodein: Kodein) {
    private val consumers = mutableMapOf<String, Consumer>()
    private val client by kodein.instance<EventClient>()

    fun setup() {
        findConsumers()
        subscribe()
    }

    private fun subscribe() {
        consumers.forEach { (topic, consumer) ->
            client.eventSubscribe(topic, consumer)
        }
    }

    private fun findConsumers() {
        val basePackageName = getBasePackageName()
        val reflections = Reflections(basePackageName)
        val annotated = reflections.getTypesAnnotatedWith(EventController::class.java)

        annotated.forEach {
            println(it.simpleName)
            val instance by kodein.Instance(TT(it))
            it.methods.filter { c -> c.isAnnotationPresent(Event::class.java) }
                .forEach { m ->
                    println("   ${m.name}")
                    var i = 0
                    val payloadTypeIndex = m.parameterAnnotations.mapNotNull { annotations ->
                        if (annotations.size == 1 && annotations.first().annotationClass == EventBody::class) {
                            i
                        } else {
                            i++
                            null
                        }
                    }
                    var payloadType: Class<*>? = null
                    if (payloadTypeIndex.size == 1) {
                        payloadType = m.parameters[payloadTypeIndex.first()].type
                    }
                    consumers[m.getAnnotation(Event::class.java).topic] =
                        Consumer(instance, m, payloadType)

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
