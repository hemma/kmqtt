package dev.bothin.smoothmqtt.publisher

import java.lang.reflect.Method

class OnPublish(private val topic: String, private val method: Method)