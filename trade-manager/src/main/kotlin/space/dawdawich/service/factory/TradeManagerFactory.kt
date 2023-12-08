package space.dawdawich.service.factory

import com.jayway.jsonpath.ParseContext
import io.ktor.client.*
import org.springframework.stereotype.Service
import space.dawdawich.client.ByBitWebSocketClient
import space.dawdawich.repositories.ByBitApiTokensRepository
import space.dawdawich.repositories.GridTableAnalyzerRepository
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.service.ByBitHttpService
import space.dawdawich.service.PriceTickerListenerFactoryService
import space.dawdawich.service.TradeManager
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class TradeManagerFactory(
    private val priceListener: PriceTickerListenerFactoryService,
    private val analyzerRepository: GridTableAnalyzerRepository,
    private val byBitApiTokensRepository: ByBitApiTokensRepository,
    private val httpClient: HttpClient, private val jsonPath: ParseContext
) {

    fun createTradeManager(tradeManagerData: TradeManagerDocument): TradeManager {
        val apiTokens = byBitApiTokensRepository.findById(tradeManagerData.apiTokensId).orElseThrow {
            Exception("Failed to create trade manager, api tokens not found.")
        }

        val encryptor = Mac.getInstance("HmacSHA256").apply {
            init(
                SecretKeySpec(apiTokens.secretKey.toByteArray(), "HmacSHA256")
            )
        }
        val byBitHttpService = ByBitHttpService(httpClient, jsonPath, apiTokens.apiKey, encryptor)

        val tradeManager = TradeManager(tradeManagerData, priceListener, analyzerRepository, byBitHttpService)
        tradeManager.webSocketClient = ByBitWebSocketClient(
            apiTokens.apiKey,
            encryptor,
            tradeManager
        ).apply { connect() }
        return tradeManager
    }
}
