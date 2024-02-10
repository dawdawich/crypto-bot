package space.dawdawich.services

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
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
import space.dawdawich.model.RequestProfitableAnalyzer
import space.dawdawich.constants.*
import space.dawdawich.model.strategy.StrategyConfigModel
import space.dawdawich.repositories.GridTableAnalyzerRepository
import space.dawdawich.repositories.SymbolRepository
import space.dawdawich.repositories.entity.GridTableAnalyzerDocument
import space.dawdawich.model.constants.AnalyzerChooseStrategy
import space.dawdawich.strategy.strategies.GridTableStrategyRunner
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.TimeUnit

@Service
class AnalyzerService(
        private val kafkaListenerContainerFactory: ConcurrentKafkaListenerContainerFactory<String, String>,
        private val symbolRepository: SymbolRepository,
        private val gridTableAnalyzerRepository: GridTableAnalyzerRepository,
        private val mongoTemplate: MongoTemplate,
) : ConsumerSeekAware {

    companion object {
        val comparator = Comparator<Pair<String, Double>> { p1, p2 -> p1.first.compareTo(p2.first) }
    }

    private val log = KotlinLogging.logger { }

    private val priceListeners = mutableMapOf<Int, PriceTickerListener>()
    private val partitionMap: MutableMap<String, Int> =
            mutableMapOf(*symbolRepository.findAll().map { it.symbol to it.partition }.toTypedArray())
    private val analyzers: MutableList<Analyzer> = mutableListOf()
    private val moneyUpdateQueue: ConcurrentSkipListSet<Pair<String, Double>> = ConcurrentSkipListSet(comparator)
    private val middlePriceUpdateQueue: ConcurrentSkipListSet<Pair<String, Double>> = ConcurrentSkipListSet(comparator)

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
            containerFactory = "jsonKafkaListenerReplayingContainerFactory"
    )
    @SendTo(RESPONSE_PROFITABLE_ANALYZER_STRATEGY_CONFIG_TOPIC)
    fun requestMostProfitableAnalyzer(request: RequestProfitableAnalyzer): StrategyConfigModel? {
        return when (request.chooseStrategy) {
            AnalyzerChooseStrategy.MOST_STABLE -> getMostStableAnalyzerStrategyConfig(request)
            AnalyzerChooseStrategy.BIGGEST_BY_MONEY -> getBiggestByMoneyAnalyzerStrategyConfig(request)
            AnalyzerChooseStrategy.CUSTOM -> throw NotImplementedError("No such analyzer strategy : %s".format(request.chooseStrategy))
        }
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

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    private fun updateSnapshot() {
        if (analyzers.isNotEmpty()) {
            val ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, GridTableAnalyzerDocument::class.java)
            val calculationStartTime = System.currentTimeMillis()
            log.info { "Start to process analyzers stability calculations" }
            runBlocking {
                analyzers.toList().forEach { analyzer ->
                    launch {
                        analyzer.updateSnapshot()
                        val stabilityCoef = analyzer.calculateStabilityCoef()
                        ops.updateOne(
                            Query.query(Criteria.where("_id").`is`(analyzer.id)),
                            Update().set("stabilityCoef", stabilityCoef)
                        )
                    }
                }
            }
            log.info { "Finish. Time elapsed: ${System.currentTimeMillis() - calculationStartTime}" }
            ops.execute()
        }
    }

    @SuppressWarnings("kotlin:S6518")
    private fun updateList(updateList: ConcurrentSkipListSet<Pair<String, Double>>, fieldName: String) {
        val ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, GridTableAnalyzerDocument::class.java)

        while (true) {
            val pair = updateList.pollFirst() ?: break

            ops.updateOne(
                Query.query(Criteria.where("_id").`is`(pair.first)),
                Update().set(fieldName, pair.second)
            )
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

    private fun getMostStableAnalyzerStrategyConfig(request: RequestProfitableAnalyzer): StrategyConfigModel? {
        val accountAnalyzer = analyzers
            .filter { it.accountId == request.accountId }
            .toList()
            .asSequence()
            .maxBy { it.calculateStabilityCoef() }

        return if (accountAnalyzer.id != request.currentAnalyzerId && accountAnalyzer.getMoney() > request.managerMoney) {
            accountAnalyzer.getStrategyConfig()
        } else null
    }

    private fun getBiggestByMoneyAnalyzerStrategyConfig(request: RequestProfitableAnalyzer): StrategyConfigModel? {
        var accountAnalyzers = analyzers.filter { it.accountId == request.accountId }.toList()
        val maxMoney = accountAnalyzers.maxBy { analyzer -> analyzer.getMoney() }.getMoney()
        accountAnalyzers = accountAnalyzers.filter { it.getMoney() == maxMoney }
        if (accountAnalyzers.none { it.id == request.currentAnalyzerId }) {
            return accountAnalyzers
                .map { it.getStrategyConfig() }
                .filter { gridTableAnalyzerRepository.findByIdOrNull(it.id)!!.startCapital < it.money }
                .minByOrNull { it.multiplier }
        }
        return null
    }
}
