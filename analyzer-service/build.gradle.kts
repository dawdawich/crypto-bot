plugins {
    id("java")
    id("org.springframework.boot") version "3.1.5"
    kotlin("plugin.serialization") version "1.9.20"
}

group = "space.dawdawich"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.kafka:spring-kafka")
    implementation(project(":domain-service"))
    implementation(project(":commons"))
}

tasks.test {
    useJUnitPlatform()
}
