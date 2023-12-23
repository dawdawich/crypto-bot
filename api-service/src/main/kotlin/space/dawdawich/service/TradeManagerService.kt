package space.dawdawich.service

import org.springframework.stereotype.Service
import space.dawdawich.repositories.TradeManagerRepository
import space.dawdawich.repositories.entity.TradeManagerDocument
import java.util.*

@Service
class TradeManagerService(private val tradeManagerRepository: TradeManagerRepository) {

    fun createNewTraderManager(apiTokenId: String, active: Boolean, customAnalyzerId: String, accountId: String): String {
        return tradeManagerRepository.insert(TradeManagerDocument(UUID.randomUUID().toString(), accountId, apiTokenId, customAnalyzerId = customAnalyzerId, isActive = active)).id
    }

    fun findAllByAccountId(accountId: String): List<TradeManagerDocument> {
        return tradeManagerRepository.findAllByAccountId(accountId)
    }

    fun updateTradeManagerStatus(id: String, accountId: String, status: Boolean) =
        tradeManagerRepository.updateTradeManagerStatus(id, accountId, status)

    fun findManager(managerId: String, accountId: String): TradeManagerDocument? {
        return tradeManagerRepository.findByIdAndAccountId(accountId, managerId)
    }

    fun updateTradeManger(manager: TradeManagerDocument) {
        tradeManagerRepository.save(manager.apply { updateTime = System.currentTimeMillis() })
    }
}
