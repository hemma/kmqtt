package dev.bothin.micromqtt.event

import java.lang.reflect.Method

data class Producer(val topic: String)

data class Consumer(val topic: String, val payloadType: Class<*>? = null)

data class Event(val instance: Any, val method: Method, val consumer: Consumer, val producer: Producer)