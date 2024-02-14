package space.dawdawich.service

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.ACTIVATE_MANAGER_TOPIC
import space.dawdawich.constants.DEACTIVATE_MANAGER_TOPIC
import space.dawdawich.model.constants.AnalyzerChooseStrategy
import space.dawdawich.repositories.TradeManagerRepository
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.repositories.entity.constants.ManagerStatus
import java.util.*

@Service
class TradeManagerService(
    private val tradeManagerRepository: TradeManagerRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val jsonKafkaTemplate: KafkaTemplate<String, TradeManagerDocument>,
) {

    fun createNewTraderManager(
        apiTokenId: String,
        status: ManagerStatus,
        analyzerFindStrategy: AnalyzerChooseStrategy,
        customAnalyzerId: String,
        stopLoss: Int?,
        takeProfit: Int?,
        accountId: String,
    ): String = tradeManagerRepository.insert(
        TradeManagerDocument(
            UUID.randomUUID().toString(),
            accountId,
            apiTokenId,
            customAnalyzerId = customAnalyzerId,
            status = status,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            chooseStrategy = analyzerFindStrategy
        )
    ).apply {
        if (status == ManagerStatus.ACTIVE) {
            jsonKafkaTemplate.send(ACTIVATE_MANAGER_TOPIC, this)
        }
    }.id

    fun findAllByAccountId(accountId: String): List<TradeManagerDocument> =
        tradeManagerRepository.findAllByAccountId(accountId)

    fun updateTradeManagerStatus(managerId: String, accountId: String, status: ManagerStatus) {
        tradeManagerRepository.findByIdAndAccountId(managerId, accountId)?.let {
            tradeManagerRepository.updateTradeManagerStatus(it.id, status)
            it.status = status
            if (status == ManagerStatus.ACTIVE) {
                jsonKafkaTemplate.send(ACTIVATE_MANAGER_TOPIC, it)
            } else if (status == ManagerStatus.INACTIVE) {
                kafkaTemplate.send(DEACTIVATE_MANAGER_TOPIC, it.id)
            }
            it
        }
    }

    fun findManager(managerId: String, accountId: String): TradeManagerDocument? =
        tradeManagerRepository.findByIdAndAccountId(managerId, accountId)

    fun updateTradeManger(manager: TradeManagerDocument) {
        tradeManagerRepository.save(manager.apply { updateTime = System.currentTimeMillis() })
    }

    fun deleteTradeManager(managerId: String, accountId: String) {
        if (tradeManagerRepository.deleteByIdAndAccountId(managerId, accountId) > 0) {
            kafkaTemplate.send(DEACTIVATE_MANAGER_TOPIC, managerId)
        }
    }

}
