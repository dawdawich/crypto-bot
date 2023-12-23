package space.dawdawich.integration.factory

import com.jayway.jsonpath.ParseContext
import space.dawdawich.integration.client.bybit.ByBitPrivateHttpClient
import io.ktor.client.*
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
@Primary
open class PrivateHttpClientFactory(private val httpClient: HttpClient, private val jsonPath: ParseContext) {
    companion object {
        const val BYBIT_SERVER_URL = "https://api.bybit.com/v5"
        const val BYBIT_TEST_SERVER_URL = "https://api-testnet.bybit.com/v5"
    }

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
