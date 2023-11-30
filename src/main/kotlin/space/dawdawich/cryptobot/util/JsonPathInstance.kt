package space.dawdawich.cryptobot.util

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import kotlinx.serialization.json.Json

val jsonPath = JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))!!
val json = Json {
    ignoreUnknownKeys = true
}
