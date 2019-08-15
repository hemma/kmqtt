package dev.bothin.micromqtt.examples

import kotlin.random.Random

class RandomService {
    private val random = Random(1337)

    fun number(): Int {
        return random.nextInt(0, 1000)
    }
}
