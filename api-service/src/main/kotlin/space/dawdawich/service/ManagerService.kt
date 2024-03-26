package space.dawdawich.service

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.ACTIVATE_MANAGER_TOPIC
import space.dawdawich.constants.DEACTIVATE_MANAGER_TOPIC
import space.dawdawich.controller.model.manager.ManagerRequest
import space.dawdawich.controller.model.manager.ManagerResponse
import space.dawdawich.repositories.constants.ManagerStatus
import space.dawdawich.repositories.mongo.AnalyzerRepository
import space.dawdawich.repositories.mongo.ApiAccessTokenRepository
import space.dawdawich.repositories.mongo.ManagerRepository
import space.dawdawich.repositories.mongo.entity.TradeManagerDocument
import java.util.*

@Service
class ManagerService(
    private val managerRepository: ManagerRepository,
    private val apiTokenRepository: ApiAccessTokenRepository,
    private val analyzerRepository: AnalyzerRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val jsonKafkaTemplate: KafkaTemplate<String, TradeManagerDocument>,
) {

    fun createNewManager(managerRequest: ManagerRequest, accountId: String): String = managerRepository.insert(
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
        managerRepository.findAllByAccountId(accountId)
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
        managerRepository.findByIdAndAccountId(managerId, accountId)?.let {
            managerRepository.updateTradeManagerStatus(it.id, status)
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
        managerRepository.findByIdAndAccountId(managerId, accountId)

    fun updateTradeManger(manager: TradeManagerDocument) {
        managerRepository.save(manager.apply { updateTime = System.currentTimeMillis() })
    }

    fun deleteTradeManager(managerId: String, accountId: String) {
        if (managerRepository.deleteByIdAndAccountId(managerId, accountId) > 0) {
            kafkaTemplate.send(DEACTIVATE_MANAGER_TOPIC, managerId)
        }
    }
}
