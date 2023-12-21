package dawdawich.space.configuration

import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.ParseContext
import dawdawich.space.client.bybit.ByBitPublicHttpClient
import dawdawich.space.factory.PrivateHttpClientFactory.Companion.BYBIT_SERVER_URL
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ServiceConfiguration {

    @Bean
    open fun jsonPath(): ParseContext = JsonPath.using(
        com.jayway.jsonpath.Configuration.defaultConfiguration().addOptions(
            Option.SUPPRESS_EXCEPTIONS))!!

    @Bean
    open fun httpClient() = HttpClient(CIO)

    @Bean
    open fun publicBybitClient() = ByBitPublicHttpClient(BYBIT_SERVER_URL, httpClient(), jsonPath())
}
