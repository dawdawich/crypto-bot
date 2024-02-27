package space.dawdawich.service

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.ACTIVATE_MANAGER_TOPIC
import space.dawdawich.constants.DEACTIVATE_MANAGER_TOPIC
import space.dawdawich.controller.model.ManagerRequest
import space.dawdawich.controller.model.ManagerResponse
import space.dawdawich.model.constants.AnalyzerChooseStrategy
import space.dawdawich.repositories.AnalyzerRepository
import space.dawdawich.repositories.ApiAccessTokenRepository
import space.dawdawich.repositories.TradeManagerRepository
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.repositories.entity.constants.ManagerStatus
import java.util.*

@Service
class ManagerService(
    private val tradeManagerRepository: TradeManagerRepository,
    private val apiTokenRepository: ApiAccessTokenRepository,
    private val analyzerRepository: AnalyzerRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val jsonKafkaTemplate: KafkaTemplate<String, TradeManagerDocument>,
) {

    fun createNewManager(managerRequest: ManagerRequest, accountId: String): String = tradeManagerRepository.insert(
        TradeManagerDocument(
            UUID.randomUUID().toString(),
            accountId,
            managerRequest.customName,
            managerRequest.apiTokenId,
            status = managerRequest.status,
            stopLoss = managerRequest.stopLoss,
            takeProfit = managerRequest.takeProfit,
            chooseStrategy = managerRequest.analyzerChooseStrategy,
            refreshAnalyzerMinutes = managerRequest.refreshAnalyzerTime,
            folder = managerRequest.folder
        )
    ).apply {
        if (status == ManagerStatus.ACTIVE) {
            jsonKafkaTemplate.send(ACTIVATE_MANAGER_TOPIC, this)
        }
    }.id

    fun findAllByAccountId(accountId: String): List<ManagerResponse> =
        tradeManagerRepository.findAllByAccountId(accountId)
            .map { document ->
                val managerMarket = apiTokenRepository.findByIdAndAccountId(document.apiTokenId, accountId).market
                val folderId = document.folder
                val analyzersCount =
                if ("ALL".equals(folderId, true)) {
                    analyzerRepository.countByAccountIdAndIsActive(accountId, true)
                } else {
                    analyzerRepository.countActiveAnalyzersInFolder(folderId, null, emptyList())
                }

                ManagerResponse(document.id, document.customName, document.status, managerMarket.name, analyzersCount, document.stopLoss, document.takeProfit)
            }

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
