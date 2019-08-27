package dev.bothin.smoothmqtt.examples

import dev.bothin.smoothmqtt.EventProducer
import dev.bothin.smoothmqtt.EventPublisher

@EventPublisher
interface HelloPublisher {

    @EventProducer(topic = "bye/123")
    fun byeWorld(payload: NameDto)
}