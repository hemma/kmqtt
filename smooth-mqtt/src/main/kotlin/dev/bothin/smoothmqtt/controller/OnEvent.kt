package dev.bothin.smoothmqtt.controller

import java.lang.reflect.Method

class OnEvent(val topic: String, val produceTopic: String = "", val method: Method, val payload: Payload?)