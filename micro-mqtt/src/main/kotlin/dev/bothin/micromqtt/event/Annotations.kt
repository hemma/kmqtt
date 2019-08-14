package dev.bothin.micromqtt.event

@Target(AnnotationTarget.FUNCTION)
annotation class Event(val topic: String)

@Target(AnnotationTarget.CLASS)
annotation class EventController

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class EventBody

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class EventTopic