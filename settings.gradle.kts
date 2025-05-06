plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "crypto-bot"
include("event-listener")
include("domain-service")
include("commons")
include("api-service")
include("integration-service")
include("strategy-lib")
include("backtest-service")
