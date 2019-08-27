package dev.bothin.smoothmqtt.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bothin.smoothmqtt.Configuration
import dev.bothin.smoothmqtt.EventBody
import dev.bothin.smoothmqtt.EventConsumer
import dev.bothin.smoothmqtt.EventController
import dev.bothin.smoothmqtt.EventProducer
import dev.bothin.smoothmqtt.EventPublisher
import dev.bothin.smoothmqtt.KGenericContainer
import dev.bothin.smoothmqtt.SmoothProcessor
import io.mockk.junit5.MockKExtension
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import org.testcontainers.containers.Network.newNetwork
import org.testcontainers.containers.ToxiproxyContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


@ExtendWith(MockKExtension::class)
@Testcontainers
class MqttIntegrationTest {

    private val network = newNetwork()

    @Container
    private val toxiproxy = ToxiproxyContainer().withNetwork(network)

    @Container
    private val mqttContainer = KGenericContainer("eclipse-mosquitto")
        .withExposedPorts(1883)
        .waitingFor(Wait.forLogMessage(".*Config loaded from.*", 1))
        .withNetwork(network)

    private lateinit var proxy: ToxiproxyContainer.ContainerProxy

    private val testController: TestController = spyk(TestController())

    @BeforeEach
    fun setup() {
        proxy = toxiproxy.getProxy(mqttContainer, 1883)
    }

    @Nested
    inner class Event {
        @Test
        fun `when connection lost then reconnect and consume`() {
            val message = TestDto(msg = "Hello")

            val exampleController = Kodein.Module("example") {
                bind<TestController>() with singleton { testController }
            }
            val kodein = Kodein {
                import(exampleController)
                import(Configuration.smoothMqttKodein(mqttContainer.containerIpAddress, mqttContainer.getMappedPort(1883)))
            }

            SmoothProcessor.findControllers("dev.bothin.smoothmqtt.mqtt").map { it.register(kodein) }

            val client = kodein.direct.instance<SmoothMqttClient>()

            client.emit("event_controller_test/topic_consume", message)
            verify(exactly = 1, timeout = 3000) { testController.onConsume(message) }

            GlobalScope.launch {
                proxy.setConnectionCut(true)
                client.emit("event_controller_test/topic_consume", message)
            }

            GlobalScope.launch {
                delay(2000)
                proxy.setConnectionCut(false)
            }

            verify(exactly = 2, timeout = 5000) { testController.onConsume(message) }
        }
    }

    @Nested
    inner class Publish {

        private lateinit var mqttClient: MqttClient

        private val mapper = jacksonObjectMapper()

        @BeforeEach
        fun setup() {
            mqttClient = MqttClient("tcp://${mqttContainer.containerIpAddress}:${mqttContainer.getMappedPort(1883)}", "clientTest")
            mqttClient.connect()
        }

        @Test
        fun `when connection lost then publish`() {
            val message = TestDto(msg = "Hello")

            val exampleController = Kodein.Module("example") {
                bind<TestController>() with singleton { testController }
            }
            val proxies = SmoothProcessor.findPublishers("dev.bothin.smoothmqtt.mqtt").map { it.proxy() }

            val kodein = Kodein {
                import(Configuration.smoothMqttKodein(mqttContainer.containerIpAddress, mqttContainer.getMappedPort(1883)))
                import(exampleController)
                importAll(proxies)
            }
            SmoothProcessor.findControllers("dev.bothin.smoothmqtt.mqtt").map { it.register(kodein) }

            val testPublisher = kodein.direct.instance<TestPublisher>()

            GlobalScope.launch {
                delay(250)
                testPublisher.produce(message)
            }

            val listener = spyk({ _: String, _: MqttMessage ->

            })

            mqttClient.subscribe("event_publisher_test/topic_publish", listener)

            GlobalScope.launch {
                proxy.setConnectionCut(true)
                testPublisher.produce(message)
            }
            GlobalScope.launch {
                delay(1000)
                proxy.setConnectionCut(false)
            }

            verify(exactly = 1, timeout = 7000) { listener("event_publisher_test/topic_publish", match { mapper.readValue(it.payload, TestDto::class.java) == message }) }
        }
    }
}


@EventController
internal class TestController {

    @EventConsumer(topic = "event_controller_test/topic_consume")
    fun onConsume(@EventBody payload: TestDto) {
        payload.msg
    }

    @EventConsumer(topic = "event_controller_test/topic_consume_produce")
    @EventProducer(topic = "event_controller_test/topic_consume")
    fun onConsumeProduce(@EventBody payload: TestDto): TestDto {
        return payload.copy(msg = "${payload.msg} Next")
    }
}

@EventPublisher
internal interface TestPublisher {

    @EventProducer(topic = "event_publisher_test/topic_publish")
    fun produce(payload: TestDto)

}

internal data class TestDto(val msg: String)
