package dev.bothin.micromqtt.examples

import dev.bothin.micromqtt.event.EventApplication
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

fun main(args: Array<String>) {
    val example = Kodein.Module("example") {
        bind<RandomService>() with singleton { RandomService() }
        bind<EventController>() with singleton { EventController(instance()) }
    }

    val app = EventApplication(listOf(example))
    app.run()
}