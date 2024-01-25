import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "space.dawdawich"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":domain-service"))
    implementation(project(":commons"))
    implementation(project(":integration-service"))

    implementation(libs.bundles.spring.boot.web)
    implementation(libs.spring.boot.security)
    implementation(libs.spring.boot.mongo)
    implementation(libs.spring.kafka)
    implementation(libs.spring.websocket)

    implementation(libs.websocket)
    implementation(libs.jackson.kotlin.module)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines)

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
