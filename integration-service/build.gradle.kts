group = "space.dawdawich"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":commons"))

    implementation(libs.json.path)
    implementation(libs.bundles.ktor)
    implementation(libs.kotlinx.serialization)
    implementation(libs.spring.boot.starter)
}

tasks.bootJar {
    enabled = false
}
