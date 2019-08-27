package dev.bothin.smoothmqtt

import dev.bothin.smoothmqtt.controller.Controller
import dev.bothin.smoothmqtt.controller.SmoothController
import dev.bothin.smoothmqtt.publisher.Publisher
import dev.bothin.smoothmqtt.publisher.SmoothPublisher

class SmoothProcessor {

    companion object {

        fun findControllers(packageName: String): List<Controller> {
            return SmoothController.findControllers(packageName)
        }

        fun findPublishers(packageName: String): List<Publisher> {
            return SmoothPublisher.findPublishers(packageName)
        }
    }
}

