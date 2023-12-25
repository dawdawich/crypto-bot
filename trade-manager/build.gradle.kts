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
    implementation(project(":domain-service"))
    implementation(project(":commons"))
    implementation(project(":integration-service"))

    implementation("org.java-websocket:Java-WebSocket")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation(group = "com.jayway.jsonpath", name = "json-path", version = "2.8.0")
    implementation("org.json:json:20231013")

    implementation("io.ktor:ktor-client-core:2.3.5")
    implementation("io.ktor:ktor-client-cio:2.3.5")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    enabled = false
}
