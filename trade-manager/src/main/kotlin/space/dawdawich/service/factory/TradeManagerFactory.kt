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
import space.dawdawich.repositories.mongo.ApiAccessTokenRepository
import space.dawdawich.repositories.mongo.entity.TradeManagerDocument
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
        val apiToken = apiAccessTokenRepository.findById(tradeManagerData.apiTokenId).orElseThrow {
            Exception("Failed to create trade manager, api tokens not found.")
        }

        val encryptor = Mac.getInstance("HmacSHA256").apply {
            init(
                SecretKeySpec(apiToken.secretKey.toByteArray(), "HmacSHA256")
            )
        }

        return Manager(
            tradeManagerData,
            serviceFactory.createHttpClient(apiToken.demoAccount, apiToken.apiKey, encryptor, apiToken.market),
            strategyConfigReplyingTemplate,
            strategyRuntimeDataReplyingTemplate,
            ByBitWebSocketClient(
                apiToken.demoAccount,
                apiToken.apiKey,
                encryptor,
                jsonPath
            ).apply { connect() },
            priceListenerFactoryService,
            apiToken.market,
            apiToken.demoAccount
        )
    }
}
