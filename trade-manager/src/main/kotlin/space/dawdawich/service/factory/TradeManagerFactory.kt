package space.dawdawich.service.factory

import com.jayway.jsonpath.ParseContext
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate
import space.dawdawich.integration.factory.PrivateHttpClientFactory
import org.springframework.stereotype.Service
import space.dawdawich.client.ByBitWebSocketClient
import space.dawdawich.managers.Manager
import space.dawdawich.model.strategy.StrategyConfigModel
import space.dawdawich.repositories.ApiAccessTokenRepository
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.service.PriceTickerListenerFactoryService
import space.dawdawich.service.TradeManagerService
import space.dawdawich.strategy.strategies.GridTableStrategyRunner
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class TradeManagerFactory(
    private val apiAccessTokenRepository: ApiAccessTokenRepository,
    private val jsonPath: ParseContext,
    private val serviceFactory: PrivateHttpClientFactory,
    private val replyingKafkaTemplate: ReplyingKafkaTemplate<String, String, StrategyConfigModel?>,
    private val priceListenerFactoryService: PriceTickerListenerFactoryService
) {

    fun createTradeManager(
        tradeManagerData: TradeManagerDocument,
        managerService: TradeManagerService
    ): Manager<GridTableStrategyRunner> {
        val apiTokens = apiAccessTokenRepository.findById(tradeManagerData.apiTokensId).orElseThrow {
            Exception("Failed to create trade manager, api tokens not found.")
        }

        val encryptor = Mac.getInstance("HmacSHA256").apply {
            init(
                SecretKeySpec(apiTokens.secretKey.toByteArray(), "HmacSHA256")
            )
        }

        return Manager(
            tradeManagerData,
            serviceFactory.createHttpClient(apiTokens.test, apiTokens.apiKey, encryptor),
            replyingKafkaTemplate,
            ByBitWebSocketClient(
                apiTokens.test,
                apiTokens.apiKey,
                encryptor,
                jsonPath
            ),
            priceListenerFactoryService
        )
    }
}
