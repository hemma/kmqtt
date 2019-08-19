package dev.bothin.smoothmqtt.examples

import dev.bothin.smoothmqtt.event.EventBody
import dev.bothin.smoothmqtt.event.EventConsumer
import dev.bothin.smoothmqtt.event.EventController
import dev.bothin.smoothmqtt.event.EventProducer
import dev.bothin.smoothmqtt.event.EventTopic

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