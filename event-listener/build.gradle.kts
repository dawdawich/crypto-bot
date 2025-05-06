import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "space.dawdawich"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":domain-service"))
    implementation(project(":commons"))
    implementation(project(":integration-service"))

    implementation(libs.websocket)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.json.path)

    implementation(libs.bundles.spring.boot.web)
    implementation(libs.spring.boot.mongo)
    implementation(libs.spring.rabbitmq)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_22)
    }
}
