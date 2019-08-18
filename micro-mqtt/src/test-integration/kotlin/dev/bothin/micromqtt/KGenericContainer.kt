package dev.bothin.micromqtt

import org.testcontainers.containers.GenericContainer

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)