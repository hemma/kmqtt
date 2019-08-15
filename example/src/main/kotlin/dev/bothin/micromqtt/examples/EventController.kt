package dev.bothin.micromqtt.examples

import dev.bothin.micromqtt.event.EventBody
import dev.bothin.micromqtt.event.EventConsumer
import dev.bothin.micromqtt.event.EventController
import dev.bothin.micromqtt.event.EventProducer
import dev.bothin.micromqtt.event.EventTopic

@EventController
class EventController(private val service: RandomService) {

    @EventConsumer(topic = "test")
    @EventProducer(topic = "tested")
    fun consumeEventOnTest(@EventTopic topic: String, @EventBody payload: Dto): String {
        println("On $topic random ${service.number()} ${payload.msg}")
        return "Yellow"
    }

    @EventConsumer(topic = "test_2")
    fun consumeEventOnTest(@EventBody payload: Dto) {
        println("Random ${service.number()} ${payload.msg}")
    }

    @EventConsumer(topic = "test_3/+")
    fun consumeEventOnTest(@EventTopic topic: String) {
        println("On $topic random ${service.number()}")
    }

}

data class Dto(val msg: String)