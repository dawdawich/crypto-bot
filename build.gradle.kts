import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.22"
    kotlin("plugin.serialization") version "1.9.20"
    kotlin("plugin.spring") version "1.8.22"
    id("org.springframework.boot") version "3.1.5"
    id("io.spring.dependency-management") version "1.1.3"
}

group = "space.dawdawich"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.springframework.boot")
        plugin("io.spring.dependency-management")
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
        implementation("org.java-websocket:Java-WebSocket:1.5.4")
        // https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients
        implementation("org.apache.kafka:kafka-clients:3.6.0")
        implementation("com.jayway.jsonpath:json-path:2.8.0")


        // spring
        implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
        implementation("org.springframework.kafka:spring-kafka")
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        // **--**--**--**--**--**--**--**--**--**--**--**--**--**--**--**--
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.java-websocket:Java-WebSocket:1.5.4")
    implementation(group = "com.jayway.jsonpath", name = "json-path", version = "2.8.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("io.ktor:ktor-server-netty:2.3.5")
    implementation("io.ktor:ktor-client-core:2.3.5")
    implementation("io.ktor:ktor-client-cio:2.3.5")
    implementation ("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation ("org.slf4j:slf4j-simple:2.0.3")
    implementation("io.micrometer:micrometer-core:1.7.5")
    implementation("io.micrometer:micrometer-registry-prometheus:1.7.5")

    // https://mvnrepository.com/artifact/org.json/json
    implementation("org.json:json:20231013")
    // https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients
    implementation("org.apache.kafka:kafka-clients:3.6.0")
    // https://mvnrepository.com/artifact/com.google.protobuf/protobuf-kotlin

    // spring
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-security")
    // **--**--**--**--**--**--**--**--**--**--**--**--**--**--**--**--
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
