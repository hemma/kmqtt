package dev.bothin.micromqtt.event

import dev.bothin.micromqtt.mqtt.MicroMqttClient
import net.sf.cglib.proxy.Enhancer
import net.sf.cglib.proxy.MethodInterceptor
import net.sf.cglib.proxy.MethodProxy
import org.kodein.di.Copy
import org.kodein.di.Instance
import org.kodein.di.Kodein
import org.kodein.di.TT
import org.kodein.di.direct
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import org.reflections.Reflections
import java.lang.reflect.Method

class EventProducerMethodInterceptor(private val microMqttClient: MicroMqttClient) : MethodInterceptor {

    override fun intercept(obj: Any, method: Method, args: Array<out Any>, proxy: MethodProxy): Any? {
        if (method.isAnnotationPresent(EventProducer::class.java)) {
            val topic = method.getAnnotation(EventProducer::class.java).topic
            microMqttClient.emit(topic, args[0])
            return null
        }

        return null
    }
}


internal class EventProcessor(private val packageName: String) {
    private val eventControllers = mutableListOf<Controller>()
    private val eventPublishers = mutableListOf<Publisher>()
    private lateinit var client: EventClient

    fun setup() {
        findControllers()
        findPublishers()
    }

    fun run(kodein: Kodein): Kodein {
        client = kodein.direct.instance()
        val proxiedKodein = proxy(kodein)
        subscribe(proxiedKodein)
        return proxiedKodein
    }

    private fun subscribe(kodein: Kodein) {
        eventControllers.forEach { event ->
            val instance by kodein.Instance(TT(event.instanceType))
            val instanceEvent = event.copy(instance = instance)
            client.eventSubscribe(instanceEvent)
        }

    }

    private fun proxy(kodein: Kodein): Kodein {
        return Kodein {
            extend(kodein, copy = Copy.All)
            eventPublishers.map {
                if (it.method.isAnnotationPresent(EventProducer::class.java)) {
                    val enhancer = Enhancer()
                    enhancer.setSuperclass(it.instanceType)
                    enhancer.setCallback(EventProducerMethodInterceptor(kodein.direct.instance()))
                    val proxy = enhancer.create()

                    Bind(TT(it.instanceType)) with singleton { proxy }
                }
            }

        }
    }

    private fun findControllers() {
        println("------ Controllers ------")
        val basePackageName = packageName
        val reflections = Reflections(basePackageName)
        val annotated = reflections.getTypesAnnotatedWith(EventController::class.java)

        annotated.forEach {
            println(it.simpleName)
            it.methods.filter { clazz -> clazz.isAnnotationPresent(EventConsumer::class.java) }
                .forEach { method ->
                    println("   ${method.name}")
                    val event = createController(method, it)
                    eventControllers.add(event)
                }
        }
    }

    private fun createController(method: Method, instanceType: Class<*>): Controller {
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
        return Controller(instanceType = instanceType, method = method, consumer = consumer, producer = producer)
    }

    private fun findPublishers() {
        println("------ Publishers ------")
        val basePackageName = packageName
        val reflections = Reflections(basePackageName)
        val annotated = reflections.getTypesAnnotatedWith(EventPublisher::class.java)

        annotated.forEach {
            println(it.simpleName)
            it.methods.filter { clazz -> clazz.isAnnotationPresent(EventProducer::class.java) }
                .forEach { method ->
                    println("   ${method.name}")
                    val publisher = createPublisher(method, it)
                    eventPublishers.add(publisher)
                }
        }
    }

    private fun createPublisher(method: Method, instanceType: Class<*>): Publisher {
        val topic = method.getAnnotation(EventProducer::class.java).topic
        return Publisher(instanceType = instanceType, method = method, topic = topic)
    }
}
