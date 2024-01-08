plugins {
    id("java")
    kotlin("plugin.serialization") version "1.9.20"
}

group = "space.dawdawich"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    enabled = false
}
