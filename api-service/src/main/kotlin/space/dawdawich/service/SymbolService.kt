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

/**
 * Service class responsible for managing symbols and their information.
 *
 * @property symbolRepository The repository for SymbolInfoDocument.
 * @property apiAccessTokenRepository The repository for ApiAccessTokenDocument.
 * @property kafkaTemplate The KafkaTemplate for sending messages.
 * @property clientFactory The factory for creating PrivateHttpClient instances.
 */
@Service
class SymbolService(
    private val symbolRepository: SymbolRepository,
    private val apiAccessTokenRepository: ApiAccessTokenRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val clientFactory: PrivateHttpClientFactory
) {

    /**
     * Retrieves all symbols from the symbol repository.
     *
     * @return A list of SymbolResponse objects representing the symbols.
     */
    fun getAllSymbols() = symbolRepository.findAll().map { it.toModel() }

    /**
     * Retrieves the names of all symbols from the symbol repository.
     *
     * @return A list of strings representing the symbol names.
     */
    fun getAllSymbolsName() = symbolRepository.findAll().map { it.symbol }

    /**
     * Adds a new symbol to the symbol repository and sends a message to reinitialize symbols.
     *
     * @param accountId The ID of the account.
     * @param symbol The symbol to add.
     */
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
                symbolInfo.maxLeverage,
                symbolInfo.qtyStep
            )
        )

        kafkaTemplate.send(SYMBOL_REINITIALIZE_TOPIC, null)
    }

    /**
     * Converts a [SymbolInfoDocument] object to a SymbolResponse object.
     *
     * @return A [SymbolResponse] object representing the converted data.
     */
    private fun SymbolInfoDocument.toModel() =
        SymbolResponse(symbol, partition, minPrice, maxPrice, tickSize, minOrderQty, maxOrderQty, qtyStep)
}
