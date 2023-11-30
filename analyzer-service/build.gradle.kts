plugins {
    id("java")
    id("org.springframework.boot") version "3.1.5"
}

group = "space.dawdawich"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.kafka:spring-kafka")
    implementation(project(":domain-service"))
    implementation(project(":commons"))
}

tasks.test {
    useJUnitPlatform()
}
