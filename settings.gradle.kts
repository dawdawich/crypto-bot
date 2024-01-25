plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "crypto-bot"
include("event-listener")
include("analyzer-service")
include("domain-service")
include("commons")
include("api-service")
include("trade-manager")
include("integration-service")
include("strategy-lib")
