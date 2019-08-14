package dev.bothin.micromqtt.examples

import dev.bothin.micromqtt.event.EventBody
import dev.bothin.micromqtt.event.Event
import dev.bothin.micromqtt.event.EventController
import dev.bothin.micromqtt.event.EventTopic
import dev.bothin.micromqtt.mqtt.MicroMqttClient

@EventController
class EventController(private val service: RandomService, private val microMqttClient: MicroMqttClient) {

    @Event(topic = "test/+")
    fun consumeEventOnTest(@EventTopic topic: String, @EventBody payload: Dto) {
        println("On $topic random ${service.number()} ${payload.msg}")
        microMqttClient.emit("response", Dto("yup"))
    }

    @Event(topic = "test_2")
    fun consumeEventOnTest(@EventBody payload: Dto) {
        println("Random ${service.number()} ${payload.msg}")
    }

    @Event(topic = "test_3")
    fun consumeEventOnTest(@EventTopic topic: String) {
        println("On $topic random ${service.number()}")
    }

}

data class Dto(val msg: String)