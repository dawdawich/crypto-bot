package space.dawdawich.service.factory

import com.jayway.jsonpath.ParseContext
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate
import space.dawdawich.integration.factory.PrivateHttpClientFactory
import org.springframework.stereotype.Service
import space.dawdawich.client.ByBitWebSocketClient
import space.dawdawich.managers.Manager
import space.dawdawich.model.RequestProfitableAnalyzer
import space.dawdawich.model.strategy.StrategyConfigModel
import space.dawdawich.model.strategy.StrategyRuntimeInfoModel
import space.dawdawich.repositories.ApiAccessTokenRepository
import space.dawdawich.repositories.entity.TradeManagerDocument
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class TradeManagerFactory(
    private val apiAccessTokenRepository: ApiAccessTokenRepository,
    private val jsonPath: ParseContext,
    private val serviceFactory: PrivateHttpClientFactory,
    private val strategyConfigReplyingTemplate: ReplyingKafkaTemplate<String, RequestProfitableAnalyzer, StrategyConfigModel?>,
    private val strategyRuntimeDataReplyingTemplate: ReplyingKafkaTemplate<String, String, StrategyRuntimeInfoModel?>,
    private val priceListenerFactoryService: PriceTickerListenerFactoryService
) {

    fun createTradeManager(tradeManagerData: TradeManagerDocument): Manager {
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
            strategyConfigReplyingTemplate,
            strategyRuntimeDataReplyingTemplate,
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
