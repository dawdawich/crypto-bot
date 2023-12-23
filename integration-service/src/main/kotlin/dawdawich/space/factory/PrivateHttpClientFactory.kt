package dawdawich.space.factory

import com.jayway.jsonpath.ParseContext
import dawdawich.space.client.bybit.ByBitPrivateHttpClient
import io.ktor.client.*
import org.springframework.stereotype.Service
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class PrivateHttpClientFactory(private val httpClient: HttpClient, private val jsonPath: ParseContext) {
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
