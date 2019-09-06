# KMqtt

KMqtt is a kotlin library for communicating over mqtt, built upon paho mqtt.

## Installation

Gradle

```gradle
implementation 'dev.bothin.kmqtt:kmqtt:0.1.0'
```

## Usage

See,

[example](https://github.com/hemma/kmqtt/blob/master/example/src/main/kotlin/dev/bothin/kmqtt/examples/Main.kt)

```kotlin
val mqttClient = MqttClient("tcp://localhost:1883", "clientId", MemoryPersistence())
val kMqttClient = KMqttClient(mqttClient)
kMqttClient.connect()

val kMqtt = KMqtt(kMqttClient)

val app = KMqttApplication(kMqttClient)
app {
    subscribe<Person>("hello/1") {
        println("Hello ${it.payload.name}")
        OutMessage.nothing()
    }

    subscribe<Person>("hello/+", "hello/response") {
        println("Hello + ${it.payload.name}")
        OutMessage(payload = it.payload.copy(name = "Kmqtt"), topicSuffix = "id")
    }

    subscribe<Any>("#") {
        println("Log: ${it.payload} on ${it.topic}")
        OutMessage.nothing()
    }
}

kMqtt.emit("hello/1", Person("Stefan"))

runBlocking {
    println("WaitReceive ${kMqtt.waitReceive<Person>("hello/response/+")}")

    println("EmitWaitReceive: ${kMqtt.emitWaitReceive("self", Person("Who?"), "self")}")
}


runBlocking {
    delay(1000)
    kMqttClient.unsubscribeAll()
    kMqttClient.disconnect()
}
```


## License
[MIT](https://choosealicense.com/licenses/mit/)
