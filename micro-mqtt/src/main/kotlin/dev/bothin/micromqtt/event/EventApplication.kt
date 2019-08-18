package dev.bothin.micromqtt.event

import dev.bothin.micromqtt.Configuration
import org.kodein.di.Kodein

class EventApplication(private val kodeinModules: List<Kodein.Module>, packageName: String, private val mqttHost: String, private val mqttPort: Int) {

    private lateinit var kodein: Kodein

    private val eventProcessor = EventProcessor(packageName)

    fun run(): Kodein {
        kodein = Kodein {
            import(Configuration.microMqttKodein(mqttHost, mqttPort))
            importAll(kodeinModules)
        }
        eventProcessor.setup()
        return eventProcessor.run(kodein)
    }
}
