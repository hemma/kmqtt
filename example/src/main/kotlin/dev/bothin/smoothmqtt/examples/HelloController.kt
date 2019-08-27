package dev.bothin.smoothmqtt.examples

import dev.bothin.smoothmqtt.EventBody
import dev.bothin.smoothmqtt.EventConsumer
import dev.bothin.smoothmqtt.EventController
import dev.bothin.smoothmqtt.EventProducer
import dev.bothin.smoothmqtt.EventTopic

@EventController
class HelloController {

    @EventConsumer(topic = "hello")
    @EventProducer(topic = "hello/answer")
    fun onHelloNameAnswer(@EventBody payload: NameDto): HelloDto {
        println("onHelloNameAnswer $payload")
        return HelloDto(msg = "Hello ${payload.name}")
    }

    @EventConsumer(topic = "bye/+")
    fun onByeName(@EventTopic topic: String, @EventBody payload: NameDto) {
        println("onByeName $payload on $topic")
    }

}

data class NameDto(val name: String)

data class HelloDto(val msg: String)