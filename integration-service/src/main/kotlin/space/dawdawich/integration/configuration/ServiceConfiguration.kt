package space.dawdawich.integration.configuration

import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.ParseContext
import space.dawdawich.integration.client.bybit.ByBitPublicHttpClient
import space.dawdawich.integration.factory.PrivateHttpClientFactory.Companion.BYBIT_SERVER_URL
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ServiceConfiguration {

    @Bean
    fun jsonPath(): ParseContext = JsonPath.using(
        com.jayway.jsonpath.Configuration.defaultConfiguration().addOptions(
            Option.SUPPRESS_EXCEPTIONS))!!

    @Bean
    fun httpClient() = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 1000
        }
    }

    @Bean
    fun publicBybitClient() = ByBitPublicHttpClient(BYBIT_SERVER_URL, httpClient(), jsonPath())

//    @Bean
//    open fun publicBybitTestClient() = ByBitPublicHttpClient(BYBIT_TEST_SERVER_URL, httpClient(), jsonPath())
}
