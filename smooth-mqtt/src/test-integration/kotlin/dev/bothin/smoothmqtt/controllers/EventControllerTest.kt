package dev.bothin.smoothmqtt.controllers

import dev.bothin.smoothmqtt.KGenericContainer
import dev.bothin.smoothmqtt.event.EventApplication
import dev.bothin.smoothmqtt.event.EventBody
import dev.bothin.smoothmqtt.event.EventConsumer
import dev.bothin.smoothmqtt.event.EventController
import dev.bothin.smoothmqtt.event.EventProducer
import dev.bothin.smoothmqtt.mqtt.SmoothMqttClient
import io.mockk.junit5.MockKExtension
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
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

    @BeforeEach
    fun setup() {
    }

    @Test
    fun `when consume annotation then listen on topic`() {
        val message = TestDto(msg = "Hello")

        val exampleController = Kodein.Module("example") {
            bind<TestController>() with singleton { testController }
        }
        val app = EventApplication(listOf(exampleController), "dev.bothin.smoothmqtt.controllers", mqttContainer.containerIpAddress, mqttContainer.getMappedPort(1883))
        val kodein = app.run()

        val client = kodein.direct.instance<SmoothMqttClient>()
        client.emit("event_controller_test/topic_consume", message)

        verify(exactly = 1, timeout = 1000) { testController.onConsume(message) }
    }

    @Test
    fun `when consume and produce annotation then listen and emit on topics`() {
        val message = TestDto(msg = "Hello")
        val messageNext = TestDto(msg = "Hello Next")

        val exampleController = Kodein.Module("example") {
            bind<TestController>() with singleton { testController }
        }
        val app = EventApplication(listOf(exampleController), "dev.bothin.smoothmqtt.controllers", mqttContainer.containerIpAddress, mqttContainer.getMappedPort(1883))
        val kodein = app.run()

        val client = kodein.direct.instance<SmoothMqttClient>()
        client.emit("event_controller_test/topic_consume_produce", message)

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
