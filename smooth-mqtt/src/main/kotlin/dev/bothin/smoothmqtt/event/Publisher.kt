package dev.bothin.smoothmqtt.event

import java.lang.reflect.Method

data class Publisher(val instanceType: Class<*>, val method: Method, val topic: String)