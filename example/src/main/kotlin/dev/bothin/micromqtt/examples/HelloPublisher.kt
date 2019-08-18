package dev.bothin.micromqtt.examples

import dev.bothin.micromqtt.event.EventProducer
import dev.bothin.micromqtt.event.EventPublisher

@EventPublisher
interface HelloPublisher {

    @EventProducer(topic = "bye/123")
    fun byeWorld(payload: NameDto)
}