package dev.bothin.smoothmqtt.examples

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HelloService(private val helloPublisher: HelloPublisher) {

    fun sendForeverEvery(delay: Long) {
        GlobalScope.launch {
            while (true) {
                kotlinx.coroutines.delay(delay)
                helloPublisher.byeWorld(NameDto("name"))
            }
        }
    }
}