package dev.bothin.smoothmqtt.examples

import dev.bothin.smoothmqtt.event.EventProducer
import dev.bothin.smoothmqtt.event.EventPublisher

@EventPublisher
interface HelloPublisher {

    @EventProducer(topic = "bye/123")
    fun byeWorld(payload: NameDto)
}