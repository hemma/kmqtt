package dev.bothin.micromqtt.examples

import kotlin.random.Random

class RandomService {
    fun number(): Int {
        return Random(1337).nextInt(0, 10)
    }
}
