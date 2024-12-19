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

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("fat")

    manifest {
        attributes["Main-Class"] = "lama.fuzzer.MainKt"
    }

    from(sourceSets["main"].output)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from({
        configurations.runtimeClasspath.get().filter { it.exists() }.map { file ->
            if (file.isDirectory) file else zipTree(file)
        }
    })
}
