package space.dawdawich.service.factory

import com.jayway.jsonpath.ParseContext
import space.dawdawich.integration.factory.PrivateHttpClientFactory
import org.springframework.stereotype.Service
import space.dawdawich.client.ByBitWebSocketClient
import space.dawdawich.repositories.ApiAccessTokenRepository
import space.dawdawich.repositories.GridTableAnalyzerRepository
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.service.PriceTickerListenerFactoryService
import space.dawdawich.service.TradeManager
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class TradeManagerFactory(
    private val priceListener: PriceTickerListenerFactoryService,
    private val analyzerRepository: GridTableAnalyzerRepository,
    private val apiAccessTokenRepository: ApiAccessTokenRepository,
    private val jsonPath: ParseContext,
    private val serviceFactory: PrivateHttpClientFactory
) {

    fun createTradeManager(tradeManagerData: TradeManagerDocument): TradeManager {
        val apiTokens = apiAccessTokenRepository.findById(tradeManagerData.apiTokensId).orElseThrow {
            Exception("Failed to create trade manager, api tokens not found.")
        }

        val encryptor = Mac.getInstance("HmacSHA256").apply {
            init(
                SecretKeySpec(apiTokens.secretKey.toByteArray(), "HmacSHA256")
            )
        }

        val tradeManager = TradeManager(tradeManagerData, priceListener, analyzerRepository, serviceFactory.createHttpClient(apiTokens.test, apiTokens.apiKey, encryptor))
        tradeManager.webSocketClient = ByBitWebSocketClient(
            apiTokens.test,
            apiTokens.apiKey,
            encryptor,
            jsonPath,
            tradeManager
        ).apply { connect() }
        return tradeManager
    }
}
