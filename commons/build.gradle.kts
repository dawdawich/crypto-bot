group = "space.dawdawich"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.serialization)
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    enabled = false
}
