import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
    implementation(libs.spring.rabbitmq)
    implementation(libs.spring.websocket)

    implementation(libs.web3)
    implementation(libs.websocket)
    implementation(libs.jackson.kotlin.module)
    implementation(libs.json.path)

    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines)

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_22)
    }
}
