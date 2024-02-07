package space.dawdawich.services

import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.repository.findByIdOrNull
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.kafka.support.TopicPartitionOffset
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import space.dawdawich.analyzers.Analyzer
import space.dawdawich.constants.*
import space.dawdawich.model.strategy.StrategyConfigModel
import space.dawdawich.repositories.GridTableAnalyzerRepository
import space.dawdawich.repositories.SymbolRepository
import space.dawdawich.repositories.entity.GridTableAnalyzerDocument
import space.dawdawich.strategy.strategies.GridTableStrategyRunner
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

@Service
class AnalyzerService(
    private val kafkaListenerContainerFactory: ConcurrentKafkaListenerContainerFactory<String, String>,
    private val symbolRepository: SymbolRepository,
    private val gridTableAnalyzerRepository: GridTableAnalyzerRepository,
    private val mongoTemplate: MongoTemplate,
) : ConsumerSeekAware {

    private val priceListeners = mutableMapOf<Int, PriceTickerListener>()
    private val partitionMap: MutableMap<String, Int> =
        mutableMapOf(*symbolRepository.findAll().map { it.symbol to it.partition }.toTypedArray())
    private val analyzers: MutableList<Analyzer> = mutableListOf()
    private val moneyUpdateQueue: ArrayBlockingQueue<Pair<String, Double>> = ArrayBlockingQueue(1_000_000)
    private val middlePriceUpdateQueue: ArrayBlockingQueue<Pair<String, Double>> = ArrayBlockingQueue(1_000_000)

    init {
        gridTableAnalyzerRepository.findAll().filter { it.isActive }.map { it.convert() }.forEach { addAnalyzer(it) }
    }

    override fun onPartitionsAssigned(
        assignments: MutableMap<org.apache.kafka.common.TopicPartition, Long>,
        callback: ConsumerSeekAware.ConsumerSeekCallback,
    ) {
        callback.seekToEnd(assignments.keys)
    }

    @KafkaListener(topics = [DEACTIVATE_ANALYZER_TOPIC])
    fun deactivateAnalyzer(analyzerId: String) {
        removeAnalyzer(analyzerId)
    }

    @KafkaListener(topics = [ACTIVATE_ANALYZER_TOPIC])
    fun activateAnalyzer(analyzerId: String) {
        gridTableAnalyzerRepository.findByIdOrNull(analyzerId)?.let {
            if (it.isActive) {
                addAnalyzer(it.convert())
            }
        }
    }

    @KafkaListener(topics = [DELETE_ANALYZER_TOPIC])
    fun deleteAnalyzer(analyzerId: String) {
        gridTableAnalyzerRepository.deleteById(analyzerId)
        removeAnalyzer(analyzerId)
    }

    @KafkaListener(
        topics = [REQUEST_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC],
        containerFactory = "kafkaListenerReplayingContainerFactory"
    )
    @SendTo(RESPONSE_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC)
    fun requestAnalyzerData(analyzerId: String) =
        analyzers.find { analyzerId == it.id }?.getRuntimeInfo()

    @KafkaListener(
        topics = [REQUEST_PROFITABLE_ANALYZER_STRATEGY_CONFIG_TOPIC],
        containerFactory = "kafkaListenerReplayingContainerFactory"
    )
    @SendTo(RESPONSE_PROFITABLE_ANALYZER_STRATEGY_CONFIG_TOPIC)
    fun requestMostProfitableAnalyzer(request: String): StrategyConfigModel? {
        val splitRequest = request.split(":").toTypedArray()
        var accountAnalyzers = analyzers.filter { it.accountId == splitRequest[0] }.toList()
        val maxMoney = accountAnalyzers.maxBy { analyzer -> analyzer.getMoney() }.getMoney()
        accountAnalyzers = accountAnalyzers.filter { it.getMoney() == maxMoney }
        if (splitRequest[1].isBlank() || (splitRequest[1].isNotBlank() && accountAnalyzers.none { it.id == splitRequest[1] })) {
            return accountAnalyzers
                .map { it.getStrategyConfig() }
                .filter { gridTableAnalyzerRepository.findByIdOrNull(it.id)!!.startCapital < it.money }
                .minByOrNull { it.multiplier }
        }
        return null
    }


    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    private fun processMiddlePriceUpdateList() {
        if (middlePriceUpdateQueue.isNotEmpty()) {
            updateList(middlePriceUpdateQueue, "middlePrice")
        }
        if (moneyUpdateQueue.isNotEmpty()) {
            updateList(moneyUpdateQueue, "money")
        }
    }

    @SuppressWarnings("kotlin:S6518")
    private fun updateList(updateList: ArrayBlockingQueue<Pair<String, Double>>, fieldName: String) {
        val ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, GridTableAnalyzerDocument::class.java)

        var pair = updateList.poll()

        while (pair != null) {
            ops.updateOne(
                Query.query(Criteria.where("_id").`is`(pair.first)),
                Update().set(fieldName, pair.second)
            )
            pair = updateList.poll()
        }
        ops.execute()
    }

    private fun addAnalyzer(analyzer: Analyzer) {
        val partition = partitionMap.getOrPut(analyzer.symbol) {
            symbolRepository.findByIdOrNull(analyzer.symbol)?.partition
                ?: throw Exception("Could not find partition for provided symbol: '${analyzer.symbol}'")
        }

        priceListeners.getOrPut(partition) {
            PriceTickerListener(
                kafkaListenerContainerFactory.createContainer(
                    TopicPartitionOffset(
                        BYBIT_TICKER_TOPIC, // TODO need to extract and process on fly
                        partition,
                        TopicPartitionOffset.SeekPosition.END
                    )
                )
            )
        }.addObserver(analyzer::acceptPriceChange)

        analyzers += analyzer
    }

    private fun removeAnalyzer(analyzerId: String) {
        analyzers.find { it.id == analyzerId }?.let {
            val partition = partitionMap[it.symbol]
            priceListeners[partition]?.removeObserver(it::acceptPriceChange)
        }

        analyzers.removeIf { it.id == analyzerId }
    }

    private fun GridTableAnalyzerDocument.convert(): Analyzer = Analyzer(
        GridTableStrategyRunner(
            symbolInfo.symbol,
            diapason,
            gridSize,
            positionStopLoss,
            positionTakeProfit,
            multiplayer,
            money,
            symbolInfo.tickSize,
            symbolInfo.minOrderQty,
            true,
            moneyChangePostProcessFunction = { _, newValue -> moneyUpdateQueue += id to newValue },
            updateMiddlePrice = { middlePrice -> middlePriceUpdateQueue += id to middlePrice },
            id = id
        ),
        0.0,
        symbolInfo.symbol,
        accountId,
        id
    )
}
