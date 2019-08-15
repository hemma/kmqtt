package dev.bothin.micromqtt.examples

import dev.bothin.micromqtt.event.EventBody
import dev.bothin.micromqtt.event.EventConsumer
import dev.bothin.micromqtt.event.EventController
import dev.bothin.micromqtt.event.EventProducer
import dev.bothin.micromqtt.event.EventTopic

@EventController
class HelloController {

    @EventConsumer(topic = "hello")
    @EventProducer(topic = "hello/answer")
    fun onHelloNameAnswer(@EventBody payload: NameDto): HelloDto {
        return HelloDto(msg = "Hello ${payload.name}")
    }

    @EventConsumer(topic = "bye/+")
    fun onByeName(@EventTopic topic: String, @EventBody payload: NameDto) {
        println("Bye ${payload.name} on $topic")
    }

}

data class NameDto(val name: String)

data class HelloDto(val msg: String)