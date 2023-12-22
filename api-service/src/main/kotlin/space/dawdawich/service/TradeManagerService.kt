package space.dawdawich.service

import org.springframework.stereotype.Service
import space.dawdawich.repositories.TradeManagerRepository
import space.dawdawich.repositories.entity.TradeManagerDocument
import java.util.*

@Service
class TradeManagerService(private val tradeManagerRepository: TradeManagerRepository) {

    fun createNewTraderManager(apiTokenId: String, accountId: String): String {
        return tradeManagerRepository.insert(TradeManagerDocument(UUID.randomUUID().toString(), accountId, apiTokenId)).id
    }

    fun findAll(): MutableList<TradeManagerDocument> {
        return tradeManagerRepository.findAll()
    }

    fun activateTradeManager(id: String) {
        tradeManagerRepository.updateTradeManagerStatus(id, true)
    }

    fun findById(managerId: String): TradeManagerDocument {
        return tradeManagerRepository.findById(managerId).get()
    }

    fun updateTradeManger(manager: TradeManagerDocument) {
        tradeManagerRepository.save(manager.apply { updateTime = System.currentTimeMillis() })
    }
}
