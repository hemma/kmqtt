package dev.bothin.smoothmqtt.event

import dev.bothin.smoothmqtt.mqtt.SmoothMqttClient
import mu.KotlinLogging
import net.sf.cglib.proxy.Enhancer
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

private val log = KotlinLogging.logger { }

class EventProducerMethodInterceptor(private val smoothMqttClient: SmoothMqttClient) : MethodInterceptor {

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


internal class EventProcessor(private val packageName: String) {
    private val eventControllers = mutableListOf<Controller>()
    private val eventPublishers = mutableListOf<Publisher>()
    private lateinit var client: EventClient

    fun setup() {
        findControllers()
        findPublishers()
    }

    fun run(modules: List<Kodein.Module>): Kodein {
        val proxies = proxy()
        val kodein = Kodein {
            importAll(modules)
            import(proxies)
        }
        client = kodein.direct.instance()
        subscribe(kodein)
        return kodein
    }

    private fun subscribe(kodein: Kodein) {
        eventControllers.forEach { event ->
            val instance by kodein.Instance(TT(event.instanceType))
            val instanceEvent = event.copy(instance = instance)
            client.eventSubscribe(instanceEvent)
        }

    }

    private fun proxy(): Kodein.Module {
        return Kodein.Module("proxies") {
            eventPublishers.map {
                if (it.method.isAnnotationPresent(EventProducer::class.java)) {
                    Bind(TT(it.instanceType)) with singleton {
                        val enhancer = Enhancer()
                        enhancer.setSuperclass(it.instanceType)
                        enhancer.setCallback(EventProducerMethodInterceptor(instance()))
                        enhancer.create()
                    }
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

        println("   ${method.name} <$consumeTopic>")

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
                    val publisher = createPublisher(method, it)
                    eventPublishers.add(publisher)
                }
        }
    }

    private fun createPublisher(method: Method, instanceType: Class<*>): Publisher {
        val topic = method.getAnnotation(EventProducer::class.java).topic

        println("   ${method.name} <$topic>")

        return Publisher(instanceType = instanceType, method = method, topic = topic)
    }
}
