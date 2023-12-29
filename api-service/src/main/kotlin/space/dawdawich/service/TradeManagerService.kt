package space.dawdawich.service

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.ACTIVATE_MANAGER_TOPIC
import space.dawdawich.constants.DEACTIVATE_MANAGER_TOPIC
import space.dawdawich.repositories.TradeManagerRepository
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.repositories.entity.constants.ManagerStatus
import java.util.*

@Service
class TradeManagerService(
    private val tradeManagerRepository: TradeManagerRepository,
    private val stringKafkaTemplate: KafkaTemplate<String, String>,
    private val documentKafkaTemplate: KafkaTemplate<String, TradeManagerDocument>,

) {

    fun createNewTraderManager(
        apiTokenId: String,
        status: ManagerStatus,
        customAnalyzerId: String,
        accountId: String
    ): String {
        return tradeManagerRepository.insert(
            TradeManagerDocument(
                UUID.randomUUID().toString(),
                accountId,
                apiTokenId,
                customAnalyzerId = customAnalyzerId,
                status = status
            )
        ).id
    }

    fun findAllByAccountId(accountId: String): List<TradeManagerDocument> {
        return tradeManagerRepository.findAllByAccountId(accountId)
    }

    fun updateTradeManagerStatus(managerId: String, accountId: String, status: ManagerStatus) {
        tradeManagerRepository.findByIdAndAccountId(managerId, accountId)?.let {
            tradeManagerRepository.updateTradeManagerStatus(it.id, status)
            it.status = status
            if (status == ManagerStatus.ACTIVE) {
                documentKafkaTemplate.send(ACTIVATE_MANAGER_TOPIC, it)
            } else if (status == ManagerStatus.INACTIVE) {
                stringKafkaTemplate.send(DEACTIVATE_MANAGER_TOPIC, it.id)
            }
            it
        }
    }

    fun findManager(managerId: String, accountId: String): TradeManagerDocument? {
        return tradeManagerRepository.findByIdAndAccountId(managerId, accountId)
    }

    fun updateTradeManger(manager: TradeManagerDocument) {
        tradeManagerRepository.save(manager.apply { updateTime = System.currentTimeMillis() })
    }

    fun deleteTradeManager(managerId: String, accountId: String) {
        if (tradeManagerRepository.deleteByIdAndAccountId(managerId, accountId) > 0) {
            stringKafkaTemplate.send(DEACTIVATE_MANAGER_TOPIC, managerId)
        }
    }

}
