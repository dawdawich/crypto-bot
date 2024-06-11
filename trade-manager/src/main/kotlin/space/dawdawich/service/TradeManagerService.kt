package space.dawdawich.service

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import space.dawdawich.constants.ACTIVATE_MANAGER_TOPIC
import space.dawdawich.constants.DEACTIVATE_MANAGER_TOPIC
import space.dawdawich.managers.Manager
import space.dawdawich.repositories.mongo.ManagerRepository
import space.dawdawich.repositories.mongo.entity.TradeManagerDocument
import space.dawdawich.repositories.constants.ManagerStatus
import space.dawdawich.service.factory.TradeManagerFactory
import java.util.*

@Service
class TradeManagerService(
    private val managerRepository: ManagerRepository,
    private val tradeManagerFactory: TradeManagerFactory
) {
    private val logger = KotlinLogging.logger {}

    private val tradeManagers: MutableList<Manager> = Collections.synchronizedList(mutableListOf())

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
                tradeManagers.forEach {
                    launch { it.deactivate() }
                }
                tradeManagers.clear()
            }
        })
    }

    @KafkaListener(topics = [ACTIVATE_MANAGER_TOPIC], groupId = "manager-document-group", containerFactory = "jsonKafkaListenerContainerFactory")
    fun activateManager(managerConfig: TradeManagerDocument) {
        try {
            tradeManagers.add(tradeManagerFactory.createTradeManager(managerConfig).apply {
                setupCrashPostAction { ex -> deactivateTradeManager(getId(), ex = ex) }
            })
        } catch (e: Exception) {
            logger.error(e) { "Failed to create manager" }
            deactivateTradeManager(managerConfig.id, ex = e)
        }
    }

    @KafkaListener(topics = [DEACTIVATE_MANAGER_TOPIC], groupId = "manager-document-group")
    fun deactivateManager(managerId: String) {
        deactivateTradeManager(managerId, ManagerStatus.INACTIVE, stopDescription = "Stopped by User")
    }

    fun deactivateTradeManager(managerId: String, status: ManagerStatus = ManagerStatus.CRASHED, stopDescription: String? = null, ex: Exception? = null) {
        tradeManagers.find { it.getId() == managerId }?.let {
            if (it.active) {
                it.deactivate()
            }
            tradeManagers.remove(it)
        }
        managerRepository.updateTradeManagerStatus(managerId, status, stopDescription, ex?.message)
    }
}
