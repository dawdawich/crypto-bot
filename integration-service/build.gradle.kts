plugins {
    kotlin("plugin.serialization") version "1.9.20"
    id("io.spring.dependency-management")
    id("org.springframework.boot")
}

group = "space.dawdawich"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core:2.3.5")
    implementation("io.ktor:ktor-client-cio:2.3.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation(project(":commons"))

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.bootJar {
    enabled = false
}
