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

/**
 * The ManagerService class provides CRUD operations for managing trade managers.
 *
 * @property managerRepository The repository for managing TradeManagerDocument objects.
 * @property apiTokenRepository The repository for managing ApiAccessTokenDocument objects.
 * @property analyzerRepository The repository for managing GridTableAnalyzerDocument objects.
 * @property kafkaTemplate The Kafka template for sending Kafka messages.
 * @property jsonKafkaTemplate The Kafka template for sending JSON messages.
 */
@Service
class ManagerService(
    private val managerRepository: ManagerRepository,
    private val apiTokenRepository: ApiAccessTokenRepository,
    private val analyzerRepository: AnalyzerRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val jsonKafkaTemplate: KafkaTemplate<String, TradeManagerDocument>,
) {

    /**
     * Creates a new manager based on the provided manager request and account ID.
     *
     * @param managerRequest The manager request object containing the following parameters:
     * - [apiTokenId] The ID of the API token.
     * - [customName] The custom name for the manager (optional).
     * - [status] The status of the manager.
     * - [analyzerChooseStrategy] The strategy for choosing an analyzer's search algorithm.
     * - [refreshAnalyzerTime] The refresh time for the analyzer.
     * - [folder] The folder for the manager (default value is "ALL").
     * - [stopLoss] The stop loss value for the manager (optional).
     * - [takeProfit] The take profit value for the manager (optional).
     *
     * @param accountId The account ID associated with the manager.
     *
     * @return The ID of the newly created manager.
     */
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

    /**
     * Finds all ManagerResponse objects associated with the given account ID.
     *
     * @param accountId The account ID.
     * @return A list of ManagerResponse objects.
     */
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

    /**
     * Updates the status of a trade manager.
     *
     * @param managerId The ID of the trade manager.
     * @param accountId The ID of the account associated with the trade manager.
     * @param status The new status of the trade manager.
     */
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

    /**
     * Finds a trade manager document by manager ID and account ID.
     *
     * @param managerId The ID of the trade manager.
     * @param accountId The ID of the account associated with the trade manager.
     * @return The TradeManagerDocument object if found, null otherwise.
     */
    fun findManager(managerId: String, accountId: String): TradeManagerDocument? =
        managerRepository.findByIdAndAccountId(managerId, accountId)

    /**
     * Updates the [TradeManagerDocument] by saving it to the [managerRepository],
     * with the updateTime field set to the current system time.
     *
     * @param manager The [TradeManagerDocument] object to update.
     * @see managerRepository
     */
    fun updateTradeManger(manager: TradeManagerDocument) {
        managerRepository.save(manager.apply { updateTime = System.currentTimeMillis() })
    }

    /**
     * Deletes a trade manager based on the provided manager ID and account ID.
     *
     * @param managerId The ID of the trade manager to delete.
     * @param accountId The ID of the account associated with the trade manager.
     */
    fun deleteTradeManager(managerId: String, accountId: String) {
        if (managerRepository.deleteByIdAndAccountId(managerId, accountId) > 0) {
            kafkaTemplate.send(DEACTIVATE_MANAGER_TOPIC, managerId)
        }
    }
}
