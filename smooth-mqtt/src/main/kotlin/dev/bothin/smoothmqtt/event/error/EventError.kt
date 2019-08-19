package dev.bothin.smoothmqtt.event.error

data class EventError(val code: String, val message: String, val receivedEvent: String)