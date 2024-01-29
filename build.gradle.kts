import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.jvm.plugin)
    alias(libs.plugins.kotlinx.serialization.plugin) apply false
    alias(libs.plugins.spring.boot.plugin) apply false
    alias(libs.plugins.spring.plugin) apply false
    alias(libs.plugins.spring.dependency.plugin) apply false
}

group = "space.dawdawich"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

subprojects {
    apply {
        plugin("org.springframework.boot")
        plugin("org.jetbrains.kotlin.plugin.serialization")
        plugin("io.spring.dependency-management")
        plugin("org.jetbrains.kotlin.plugin.spring")
        plugin("org.jetbrains.kotlin.jvm")
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation(rootProject.libs.kotlin.jdk)
        implementation(rootProject.libs.logstash)
        implementation(rootProject.libs.kotlin.logging)
        implementation(rootProject.libs.kotlin.reflect)
    }
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
