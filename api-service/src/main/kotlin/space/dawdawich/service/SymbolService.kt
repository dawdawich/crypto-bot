package space.dawdawich.service

import kotlinx.coroutines.runBlocking
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import space.dawdawich.controller.model.SymbolResponse
import space.dawdawich.integration.client.PublicHttpClient
import space.dawdawich.repositories.mongo.SymbolRepository
import space.dawdawich.repositories.mongo.entity.SymbolDocument

/**
 * Service class responsible for managing symbols and their information.
 *
 * @property symbolRepository The repository for SymbolInfoDocument.
 * @property rabbitTemplate The RabbitTemplate for sending messages.
 * @property publicBybitClient http client to make request to market
 */
@Service
class SymbolService(
    private val symbolRepository: SymbolRepository,
    private val rabbitTemplate: RabbitTemplate,
    private val publicBybitClient: PublicHttpClient,
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
        val symbolInfo = runBlocking {
            publicBybitClient.getPairInstructions(symbol)
        }

        symbolRepository.insert(
            SymbolDocument(
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
    }

    /**
     * Converts a [SymbolDocument] object to a SymbolResponse object.
     *
     * @return A [SymbolResponse] object representing the converted data.
     */
    private fun SymbolDocument.toModel() =
        SymbolResponse(symbol, partition, minPrice, maxPrice, tickSize, minOrderQty, maxOrderQty, qtyStep)
}
