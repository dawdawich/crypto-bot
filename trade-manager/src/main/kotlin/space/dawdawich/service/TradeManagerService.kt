package space.dawdawich.service

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.ACTIVATE_MANAGER_TOPIC
import space.dawdawich.constants.DEACTIVATE_MANAGER_TOPIC
import space.dawdawich.constants.REQUEST_MANAGER_TOPIC
import space.dawdawich.constants.RESPONSE_MANAGER_TOPIC
import space.dawdawich.managers.Manager
import space.dawdawich.model.manager.ManagerInfoModel
import space.dawdawich.repositories.TradeManagerRepository
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.repositories.entity.constants.ManagerStatus
import space.dawdawich.service.factory.TradeManagerFactory
import java.util.*

@Service
class TradeManagerService(
    private val tradeManagerRepository: TradeManagerRepository,
    private val tradeManagerFactory: TradeManagerFactory,
    private val managerInfoKafkaTemplate: KafkaTemplate<String, ManagerInfoModel>
) {

    private val tradeManagers: MutableList<Manager<*>> = Collections.synchronizedList(mutableListOf())
    private val priceListeners = mutableMapOf<Int, PriceTickerListener>()

    init {
        // TODO: reimplement with kafka
//        mongoTemplate.changeStream<GridTableAnalyzerDocument>()
//            .watchCollection("grid_table_analyzer")
//            .listen()
//            .filter { changeStream -> changeStream.operationType == OperationType.UPDATE && tradeManagers.any { it.analyzer?.id == changeStream.body?.id } }
//            .subscribe {
//                val document = it.body
//                val manager = tradeManagers.first { analyzer -> analyzer.analyzer?.id == document?.id }
//                if (manager.middlePrice != document?.middlePrice) {
//                    manager.updateMiddlePrice(document?.middlePrice ?: -1.0)
//                }
//            }

        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
                tradeManagers.forEach {
                    launch { it.deactivate() }
                }
                tradeManagers.clear()
            }
        })
    }

    @KafkaListener(topics = [ACTIVATE_MANAGER_TOPIC], groupId = "manager-document-group", containerFactory = "managerDocumentKafkaListenerContainerFactory")
    fun activateManager(manager: TradeManagerDocument) {
        val newTradeManager = tradeManagerFactory.createTradeManager(manager, this)
        tradeManagers.add(newTradeManager)
    }

    @KafkaListener(topics = [DEACTIVATE_MANAGER_TOPIC])
    fun deactivateManager(managerId: String) {
        deactivateTradeManager(managerId, ManagerStatus.INACTIVE, stopDescription = "Stopped by User")
    }

    @KafkaListener(topics = [REQUEST_MANAGER_TOPIC])
    fun requestManagerInfo(managerId: String) {
        tradeManagers.find { manager -> managerId == manager.getId() }?.let { manager ->
//            managerInfoKafkaTemplate.send(RESPONSE_MANAGER_TOPIC, manager.getManagerInfo())
        }
    }

    fun deactivateTradeManager(managerId: String, status: ManagerStatus = ManagerStatus.CRASHED, stopDescription: String? = null, ex: Exception? = null) {
        tradeManagers.removeIf {
            if (it.getId() == managerId) {
                it.deactivate()
                return@removeIf true
            }
            return@removeIf false
        }
        tradeManagerRepository.updateTradeManagerStatus(managerId, status, stopDescription, ex?.message)
    }
}
