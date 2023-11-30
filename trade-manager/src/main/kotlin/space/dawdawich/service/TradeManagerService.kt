package space.dawdawich.service

import com.mongodb.client.model.changestream.OperationType
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.changeStream
import org.springframework.stereotype.Service
import space.dawdawich.repositories.ByBitApiTokensRepository
import space.dawdawich.repositories.GridTableAnalyzerRepository
import space.dawdawich.repositories.TradeManagerRepository
import space.dawdawich.repositories.entity.TradeManagerDocument

@Service
open class TradeManagerService(
    private val tradeManagerRepository: TradeManagerRepository,
    private val byBitApiTokensRepository: ByBitApiTokensRepository,
    private val mongoTemplate: ReactiveMongoTemplate,
    private val priceTickerListenerFactoryService: PriceTickerListenerFactoryService,
    private val analyzerRepository: GridTableAnalyzerRepository,
    private val orderService: ByBitOrderHttpService
) {

    private val tradeManagers: MutableList<TradeManager> = mutableListOf()

    init {
        tradeManagers.addAll(tradeManagerRepository.findAll().map { data ->
            val apiTokens = byBitApiTokensRepository.findById(data.apiTokensId).get()
            TradeManager(
                data,
                priceTickerListenerFactoryService,
                analyzerRepository,
                orderService,
                apiTokens.apiKey,
                apiTokens.secretKey
            )
        })

        val tradeManagerListener = mongoTemplate.changeStream<TradeManagerDocument>().listen()
        tradeManagerListener
            .filter { changeStreamEvent -> changeStreamEvent.operationType == OperationType.INSERT && changeStreamEvent.collectionName == "trade_manager" }
            .subscribe {
                val newTradeManager = it.body
                newTradeManager?.let { data ->
                    val apiTokens = byBitApiTokensRepository.findById(data.apiTokensId).get()
                    tradeManagers += TradeManager(
                        data,
                        priceTickerListenerFactoryService,
                        analyzerRepository,
                        orderService,
                        apiTokens.apiKey,
                        apiTokens.secretKey
                    )
                }
            }
        tradeManagerListener.filter { changeStreamEvent -> (changeStreamEvent.operationType == OperationType.UPDATE || changeStreamEvent.operationType == OperationType.REPLACE) && changeStreamEvent.collectionName == "trade_manager" }
            .subscribe {
                it.body?.let { changedDocument ->
                    tradeManagers.find { manager -> manager.getId() == changedDocument.id }
                        ?.updateTradeData(changedDocument)
                }
            }
    }
}
