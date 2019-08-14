package dev.bothin.micromqtt.event.error

data class EventError(val code: String, val message: String, val receivedEvent: String)