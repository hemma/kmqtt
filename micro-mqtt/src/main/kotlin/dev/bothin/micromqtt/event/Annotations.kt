package dev.bothin.micromqtt.event

@Target(AnnotationTarget.FUNCTION)
annotation class EventConsumer(val topic: String)

@Target(AnnotationTarget.FUNCTION)
annotation class EventProducer(val topic: String)

@Target(AnnotationTarget.CLASS)
annotation class EventController

@Target(AnnotationTarget.CLASS)
annotation class EventPublisher

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class EventBody

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class EventTopic