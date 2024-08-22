package space.dawdawich.service.factory

import com.jayway.jsonpath.ParseContext
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import space.dawdawich.client.ByBitWebSocketClient
import space.dawdawich.integration.factory.PrivateHttpClientFactory
import space.dawdawich.managers.CustomRSIOutOfBoundManager
import space.dawdawich.managers.Manager
import space.dawdawich.repositories.mongo.ApiAccessTokenRepository
import space.dawdawich.repositories.mongo.PositionRepository
import space.dawdawich.repositories.mongo.entity.SymbolInfoDocument
import space.dawdawich.repositories.mongo.entity.TradeManagerDocument
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class TradeManagerFactory(
    private val apiAccessTokenRepository: ApiAccessTokenRepository,
    private val jsonPath: ParseContext,
    private val serviceFactory: PrivateHttpClientFactory,
    private val rabbitTemplate: RabbitTemplate,
    private val priceListenerFactoryService: EventListenerFactoryService,
    private val positionRepository: PositionRepository
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
            rabbitTemplate,
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

    fun createCustomManager(symbols: List<SymbolInfoDocument>): CustomRSIOutOfBoundManager {
        val apiToken = apiAccessTokenRepository.findById("74c85341-c6bc-4d4b-bf8e-c7166b0d37b7").orElseThrow {
            Exception("Failed to create trade manager, api tokens not found.")
        }

        val encryptor = Mac.getInstance("HmacSHA256").apply {
            init(
                SecretKeySpec(apiToken.secretKey.toByteArray(), "HmacSHA256")
            )
        }

        return CustomRSIOutOfBoundManager(
            serviceFactory.createHttpClient(apiToken.demoAccount, apiToken.apiKey, encryptor, apiToken.market),
            ByBitWebSocketClient(
                apiToken.demoAccount,
                apiToken.apiKey,
                encryptor,
                jsonPath
            ).apply { connect() },
            priceListenerFactoryService,
            positionRepository,
            symbols.map { it.symbol },
            mapOf(*symbols.map { it.symbol to it.minOrderQty }.toTypedArray()),
            mapOf(*symbols.map { it.symbol to it.maxLeverage }.toTypedArray()),
            5.0
        )
    }
}
