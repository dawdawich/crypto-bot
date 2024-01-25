import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "space.dawdawich"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":domain-service"))
    implementation(project(":commons"))

    implementation(libs.websocket)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.json.path)

    implementation(libs.bundles.spring.boot.web)
    implementation(libs.spring.boot.mongo)
    implementation(libs.spring.kafka)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}
