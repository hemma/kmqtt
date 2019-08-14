package dev.bothin.micromqtt.event

import java.lang.reflect.Method

data class Consumer(val instance: Any, val method: Method, val payloadType: Class<*>? = null)