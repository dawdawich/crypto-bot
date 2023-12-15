package space.dawdawich.service

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.SYMBOL_REINITIALIZE_TOPIC
import space.dawdawich.controller.model.SymbolResponse
import space.dawdawich.repositories.SymbolRepository
import space.dawdawich.repositories.entity.SymbolInfoDocument

@Service
class SymbolService(private val symbolRepository: SymbolRepository, private val kafkaTemplate: KafkaTemplate<String, String>) {

    fun getAllSymbols() = symbolRepository.findAll().map { it.toModel() }

    fun getAllSymbolsName() = symbolRepository.findAll().map { it.symbol }

    fun addNewSymbol(symbol: String, isOneWayMode: Boolean, priceMinStep: Double) {
        symbolRepository.insert(SymbolInfoDocument(symbol, symbolRepository.count().toInt(), isOneWayMode, priceMinStep))

        kafkaTemplate.send(SYMBOL_REINITIALIZE_TOPIC, null)
    }

    private fun SymbolInfoDocument.toModel() = SymbolResponse(symbol, partition, isOneWayMode, priceMinStep)
}
