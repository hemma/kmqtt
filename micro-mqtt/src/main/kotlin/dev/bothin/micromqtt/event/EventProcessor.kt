package dev.bothin.micromqtt.event

import dev.bothin.micromqtt.mqtt.MicroMqttClient
import net.sf.cglib.proxy.Enhancer
import net.sf.cglib.proxy.InvocationHandler
import net.sf.cglib.proxy.MethodInterceptor
import net.sf.cglib.proxy.MethodProxy
import org.kodein.di.Instance
import org.kodein.di.Kodein
import org.kodein.di.TT
import org.kodein.di.direct
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import org.reflections.Reflections
import java.lang.reflect.Method


class EventProducerMethodInterceptor(private val microMqttClient: MicroMqttClient) : MethodInterceptor {

    override fun intercept(obj: Any, method: Method, args: Array<out Any>, proxy: MethodProxy): Any {
        if (method.isAnnotationPresent(EventProducer::class.java)) {
            val topic = method.getAnnotation(EventProducer::class.java).topic
            val payload = proxy.invokeSuper(obj, args)
            microMqttClient.emit(topic, payload)
            return payload
        }

        return proxy.invokeSuper(obj, args)
    }
}

class EventInvocationHandler : InvocationHandler {

    override operator fun invoke(proxy: Any, method: Method, args: Array<Any>): Any {
        return method.invoke(proxy, args)
    }
}

internal class EventProcessor() {
    private val eventControllers = mutableListOf<Event>()
    private lateinit var client: EventClient

    fun setup(kodein: Kodein): Kodein {
        findControllers()

        return Kodein {
            extend(kodein)
            eventControllers.map {
                val enhancer = Enhancer()
                enhancer.setSuperclass(it.instanceType)
                enhancer.setCallback(EventProducerMethodInterceptor(kodein.direct.instance()))
                val proxy = enhancer.create()


                Bind(TT(it.instanceType), overrides = true) with singleton { proxy }
            }

        }
    }

    fun run(kodein: Kodein) {
        client = kodein.direct.instance()
        subscribe(kodein)
    }

    private fun subscribe(kodein: Kodein) {
        eventControllers.forEach { event ->
            val instance by kodein.Instance(TT(event.instanceType))
            val instanceEvent = event.copy(instance = instance)
            client.eventSubscribe(instanceEvent)
        }

    }

    private fun findControllers() {
        val basePackageName = getBasePackageName()
        val reflections = Reflections(basePackageName)
        val annotated = reflections.getTypesAnnotatedWith(EventController::class.java)

        annotated.forEach {
            println(it.simpleName)
            it.methods.filter { clazz -> clazz.isAnnotationPresent(EventConsumer::class.java) }
                .forEach { method ->
                    println("   ${method.name}")
                    val event = createEvent(method, it)
                    eventControllers.add(event)
                }
        }
    }

    private fun createEvent(method: Method, instanceType: Class<*>): Event {
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
        val event = Event(instanceType = instanceType, method = method, consumer = consumer, producer = producer)
        return event
    }


    private fun getBasePackageName(): String {
        val stack = Thread.currentThread().stackTrace
        val main = stack[stack.size - 1]
        val mainClass = main.className
        val split = mainClass.split(".")
        return split.subList(0, split.size - 1).joinToString(".")
    }
}
