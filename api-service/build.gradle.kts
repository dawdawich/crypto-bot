import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation(project(":domain-service"))
    implementation(project(":commons"))
    implementation(project(":integration-service"))

    implementation("org.java-websocket:Java-WebSocket")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.springframework:spring-websocket:6.1.2")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}
