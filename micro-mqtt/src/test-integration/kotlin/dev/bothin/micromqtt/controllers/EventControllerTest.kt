package dev.bothin.micromqtt.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bothin.micromqtt.KGenericContainer
import dev.bothin.micromqtt.event.EventApplication
import dev.bothin.micromqtt.event.EventBody
import dev.bothin.micromqtt.event.EventConsumer
import dev.bothin.micromqtt.event.EventController
import dev.bothin.micromqtt.event.EventProducer
import io.mockk.junit5.MockKExtension
import io.mockk.spyk
import io.mockk.verify
import org.eclipse.paho.client.mqttv3.MqttClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@ExtendWith(MockKExtension::class)
@Testcontainers
class EventControllerTest {

    @Container
    private val mqttContainer = KGenericContainer("eclipse-mosquitto")
        .withExposedPorts(1883)
        .waitingFor(Wait.forLogMessage(".*Config loaded from.*", 1))


    private val testController: TestController = spyk(TestController())

    private lateinit var mqttClient: MqttClient

    private val mapper = jacksonObjectMapper()

    @BeforeEach
    fun setup() {
        mqttClient = MqttClient("tcp://${mqttContainer.containerIpAddress}:${mqttContainer.getMappedPort(1883)}", "clientTest")
        mqttClient.connect()
    }

    @Test
    fun `when consume annotation then listen on topic`() {
        val message = TestDto(msg = "Hello")

        val exampleController = Kodein.Module("example") {
            bind<TestController>() with singleton { testController }
        }
        val app = EventApplication(listOf(exampleController), "dev.bothin.micromqtt.controllers", mqttContainer.containerIpAddress, mqttContainer.getMappedPort(1883))
        app.run()

        mqttClient.publish("event_controller_test/topic_consume", mapper.writeValueAsBytes(message), 1, false)

        verify(exactly = 1, timeout = 1000) { testController.onConsume(message) }
    }

    @Test
    fun `when consume and produce annotation then listen and emit on topics`() {
        val message = TestDto(msg = "Hello")
        val messageNext = TestDto(msg = "Hello Next")

        val exampleController = Kodein.Module("example") {
            bind<TestController>() with singleton { testController }
        }
        val app = EventApplication(listOf(exampleController), "dev.bothin.micromqtt.controllers", mqttContainer.containerIpAddress, mqttContainer.getMappedPort(1883))
        app.run()

        mqttClient.publish("event_controller_test/topic_consume_produce", mapper.writeValueAsBytes(message), 1, false)

        verify(exactly = 1, timeout = 1000) { testController.onConsumeProduce(message) }
        verify(exactly = 1, timeout = 1000) { testController.onConsume(messageNext) }
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

internal data class TestDto(val msg: String)
