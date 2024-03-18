package space.dawdawich.service

import space.dawdawich.integration.factory.PrivateHttpClientFactory
import kotlinx.coroutines.runBlocking
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.SYMBOL_REINITIALIZE_TOPIC
import space.dawdawich.controller.model.SymbolResponse
import space.dawdawich.repositories.mongo.ApiAccessTokenRepository
import space.dawdawich.repositories.mongo.SymbolRepository
import space.dawdawich.repositories.mongo.entity.SymbolInfoDocument

@Service
class SymbolService(
    private val symbolRepository: SymbolRepository,
    private val apiAccessTokenRepository: ApiAccessTokenRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val clientFactory: PrivateHttpClientFactory
) {

    fun getAllSymbols() = symbolRepository.findAll().map { it.toModel() }

    fun getAllSymbolsName() = symbolRepository.findAll().map { it.symbol }

    fun addNewSymbol(accountId: String, symbol: String) {
        val apiToken = apiAccessTokenRepository.findAllByAccountId(accountId)[0]
        val httpClient = clientFactory.createHttpClient(apiToken.demoAccount, apiToken.apiKey, apiToken.secretKey, apiToken.market)
        val symbolInfo = runBlocking {
            httpClient.getPairInstructions(symbol)
        }

        symbolRepository.insert(
            SymbolInfoDocument(
                symbol,
                symbolRepository.count().toInt(),
                symbolInfo.tickSize,
                symbolInfo.minPrice,
                symbolInfo.maxPrice,
                symbolInfo.minOrderQty,
                symbolInfo.maxOrderQty,
                symbolInfo.qtyStep
            )
        )

        kafkaTemplate.send(SYMBOL_REINITIALIZE_TOPIC, null)
    }

    private fun SymbolInfoDocument.toModel() =
        SymbolResponse(symbol, partition, minPrice, maxPrice, tickSize, minOrderQty, maxOrderQty, qtyStep)
}
