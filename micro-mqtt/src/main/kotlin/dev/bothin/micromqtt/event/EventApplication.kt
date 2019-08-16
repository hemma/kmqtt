package dev.bothin.micromqtt.event

import dev.bothin.micromqtt.Configuration
import org.kodein.di.Kodein

class EventApplication(private val kodeinModules: List<Kodein.Module>) {

    private lateinit var kodein: Kodein

    private val eventProcessor = EventProcessor()

    fun run() {
        kodein = Kodein {
            import(Configuration.microMqttKodein)
            importAll(kodeinModules)
        }
        val extendedKodein = eventProcessor.setup(kodein)

        eventProcessor.run(extendedKodein)
    }
}
