group = "space.dawdawich"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":commons"))
    implementation(libs.spring.boot.mongo)
    implementation(libs.spring.boot.redis)
}

tasks.bootJar {
    enabled = false
}
