group = "space.dawdawich"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":domain-service"))
    implementation(project(":commons"))
    implementation(project(":strategy-lib"))

    implementation(libs.bundles.spring.boot.web)
    implementation(libs.spring.boot.mongo)
    implementation(libs.spring.kafka)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.jackson.kotlin.module)
}

tasks.test {
    useJUnitPlatform()
}
