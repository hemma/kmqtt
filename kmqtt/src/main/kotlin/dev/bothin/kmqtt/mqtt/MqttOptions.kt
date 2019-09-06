package dev.bothin.kmqtt.mqtt

data class MqttOptions(val maxInFlight: Int = 50,
                       val automaticReconnect: Boolean = true,
                       val keepAliveInterval: Int = 60,
                       val connectionTimeOut: Int = 30,
                       val username: String? = null,
                       val password: String? = null) {

    companion object {
        fun default() = MqttOptions()
    }
}