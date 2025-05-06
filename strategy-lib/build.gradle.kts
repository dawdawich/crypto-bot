group = "dawdawich.space"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":commons"))
}

tasks.bootJar {
    enabled = false
}
