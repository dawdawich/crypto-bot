package space.dawdawich.integration.factory

import com.jayway.jsonpath.ParseContext
import space.dawdawich.integration.client.bybit.ByBitPrivateHttpClient
import io.ktor.client.*
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import space.dawdawich.constants.BYBIT_SERVER_URL
import space.dawdawich.constants.BYBIT_TEST_SERVER_URL
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
@Primary
class PrivateHttpClientFactory(private val httpClient: HttpClient, private val jsonPath: ParseContext) {

    fun createHttpClient(isTest: Boolean, apiKey: String, encryptor: Mac) = ByBitPrivateHttpClient(
        if (isTest) BYBIT_TEST_SERVER_URL else BYBIT_SERVER_URL,
        httpClient,
        jsonPath,
        apiKey,
        encryptor
    )

    fun createHttpClient(isTest: Boolean, apiKey: String, secretKey: String): ByBitPrivateHttpClient {
        return createHttpClient(isTest, apiKey, Mac.getInstance("HmacSHA256").apply {
            init(
                SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
            )
        })
    }
}
