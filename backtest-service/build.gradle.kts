group = "space.dawdawich"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":domain-service"))
    implementation(project(":commons"))
    implementation(project(":strategy-lib"))

    implementation(libs.spring.boot.mongo)
    implementation(libs.spring.rabbitmq)

    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines)

    implementation(libs.jackson.kotlin.module)
}

tasks.test {
    useJUnitPlatform()
}
