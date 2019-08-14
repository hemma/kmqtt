package dev.bothin.micromqtt.event

import dev.bothin.micromqtt.Configuration
import org.kodein.di.Kodein

class EventApplication(kodeinModules: List<Kodein.Module>) {

    private val kodein = Kodein {
        import(Configuration.microMqttKodein)
        importAll(kodeinModules)
    }

    private val eventProcessor = EventProcessor(kodein)

    fun run() {
        eventProcessor.setup()
    }
}