package dev.bothin.smoothmqtt.event

import dev.bothin.smoothmqtt.Configuration
import org.kodein.di.Kodein

class EventApplication(private val kodeinModules: List<Kodein.Module>, packageName: String, private val mqttHost: String, private val mqttPort: Int) {

    private lateinit var kodein: Kodein

    private val eventProcessor = EventProcessor(packageName)

    fun run(): Kodein {
        kodein = Kodein {
            import(Configuration.smoothMqttKodein(mqttHost, mqttPort))
            importAll(kodeinModules)
        }
        eventProcessor.setup()
        return eventProcessor.run(kodein)
    }
}
