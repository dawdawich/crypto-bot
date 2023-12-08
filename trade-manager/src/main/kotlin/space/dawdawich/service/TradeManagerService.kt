package space.dawdawich.service

import com.mongodb.client.model.changestream.OperationType
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.changeStream
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
        tradeManagers.addAll(tradeManagerRepository.findAll().map { data ->
            tradeManagerFactory.createTradeManager(data)
        })

        val tradeManagerListener = mongoTemplate.changeStream<TradeManagerDocument>().listen()
        val analyzerListener = mongoTemplate.changeStream<GridTableAnalyzerDocument>().listen()
        tradeManagerListener
            .filter { changeStreamEvent -> changeStreamEvent.collectionName == "trade_manager" && changeStreamEvent.operationType == OperationType.INSERT }
            .subscribe {
                val newTradeManager = it.body
                newTradeManager?.let { data ->
                    tradeManagers += tradeManagerFactory.createTradeManager(data)
                }
            }
        tradeManagerListener.filter { changeStreamEvent -> changeStreamEvent.collectionName == "trade_manager" && (changeStreamEvent.operationType == OperationType.UPDATE || changeStreamEvent.operationType == OperationType.REPLACE) }
            .subscribe {
                it.body?.let { changedDocument ->
                    tradeManagers.find { manager -> manager.getId() == changedDocument.id }
                        ?.updateTradeData(changedDocument)
                }
            }
        analyzerListener.filter { changeStream -> changeStream.collectionName == "grid_table_analyzer" && changeStream.operationType == OperationType.UPDATE && tradeManagers.any { it.analyzer?.id == changeStream.body?.id } }
            .subscribe {
                val document = it.body
                val manager = tradeManagers.first { analyzer -> analyzer.analyzer?.id == document?.id }
                if (manager.middlePrice != document?.middlePrice) {
                    manager.updateMiddlePrice(document?.middlePrice ?: -1.0)
                }

            }

    }
}
