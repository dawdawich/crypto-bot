package space.dawdawich.configuration

import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.ParseContext
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class HttpClientConfiguration {

    @Bean
    open fun httpClient(): HttpClient = HttpClient(CIO)

    @Bean
    open fun jsonPath(): ParseContext = JsonPath.using(
        com.jayway.jsonpath.Configuration.defaultConfiguration().addOptions(
            Option.SUPPRESS_EXCEPTIONS))!!

}
