import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("plugin.serialization") version "1.9.20"
    id("io.spring.dependency-management")
    id("org.springframework.boot") version "3.1.5"
}

group = "space.dawdawich"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation("org.java-websocket:Java-WebSocket")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.apache.kafka:kafka-clients")
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.jayway.jsonpath:json-path")

    // spring
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.kafka:spring-kafka")
    // **--**--**--**--**--**--**--**--**--**--**--**--**--**--**--**--

    implementation(project(":domain-service"))
    implementation(project(":commons"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}
