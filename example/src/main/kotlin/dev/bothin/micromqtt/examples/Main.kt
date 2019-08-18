package dev.bothin.micromqtt.examples

import dev.bothin.micromqtt.event.EventApplication
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

fun main(args: Array<String>) {
    val example = Kodein.Module("example") {
        bind<HelloController>() with singleton { HelloController() }
        bind<HelloService>() with singleton { HelloService(instance()) }
    }

    val app = EventApplication(listOf(example), "dev.bothin.micromqtt.examples", "localhost", 1883)
    val kodein = app.run()

    val helloService = kodein.direct.instance<HelloService>()

    helloService.sendForeverEvery(5000)
}