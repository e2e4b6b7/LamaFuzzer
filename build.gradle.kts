plugins {
    kotlin("jvm") version "2.0.20"
}

group = "lama.fuzzer"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}
