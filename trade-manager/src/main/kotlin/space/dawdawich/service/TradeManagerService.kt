package space.dawdawich.service

import com.mongodb.client.model.changestream.OperationType
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.changeStream
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.stereotype.Service
import space.dawdawich.repositories.TradeManagerRepository
import space.dawdawich.repositories.entity.GridTableAnalyzerDocument
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.service.factory.TradeManagerFactory

@Service
open class TradeManagerService(
    tradeManagerRepository: TradeManagerRepository,
    mongoTemplate: ReactiveMongoTemplate,
    private val tradeManagerFactory: TradeManagerFactory
) {

    private val tradeManagers: MutableList<TradeManager> = mutableListOf()

    init {
        tradeManagers.addAll(tradeManagerRepository.findAllByActive().map { data ->
            tradeManagerFactory.createTradeManager(data)
        })

        mongoTemplate.changeStream<TradeManagerDocument>()
            .watchCollection("trade_manager")
            .filter(where("operationType").`is`(OperationType.INSERT))
            .listen()
            .subscribe {
                val newTradeManager = it.body
                newTradeManager?.let { data ->
                    tradeManagers += tradeManagerFactory.createTradeManager(data)
                }
            }
        mongoTemplate.changeStream<TradeManagerDocument>()
            .watchCollection("trade_manager")
            .filter(where("operationType").`in`(OperationType.UPDATE, OperationType.REPLACE))
            .listen()
            .subscribe {
                it.body?.let { changedDocument ->
                    tradeManagers.find { manager -> manager.getId() == changedDocument.id }
                        ?.updateTradeData(changedDocument)
                }
            }
        mongoTemplate.changeStream<GridTableAnalyzerDocument>()
            .watchCollection("grid_table_analyzer")
            .filter(where("operationType").`is`(OperationType.UPDATE))
            .listen()
            .filter { changeStream -> tradeManagers.any { it.analyzer?.id == changeStream.body?.id } }
            .subscribe {
                val document = it.body
                val manager = tradeManagers.first { analyzer -> analyzer.analyzer?.id == document?.id }
                if (manager.middlePrice != document?.middlePrice) {
                    manager.updateMiddlePrice(document?.middlePrice ?: -1.0)
                }
            }

    }
}
