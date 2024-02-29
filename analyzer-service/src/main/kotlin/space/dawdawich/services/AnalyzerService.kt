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
import space.dawdawich.constants.*
import space.dawdawich.model.RequestProfitableAnalyzer
import space.dawdawich.model.constants.AnalyzerChooseStrategy
import space.dawdawich.model.strategy.AnalyzerRuntimeInfoModel
import space.dawdawich.model.strategy.StrategyConfigModel
import space.dawdawich.repositories.mongo.AnalyzerRepository
import space.dawdawich.repositories.mongo.SymbolRepository
import space.dawdawich.repositories.mongo.entity.GridTableAnalyzerDocument
import space.dawdawich.repositories.redis.AnalyzerStabilityRepository
import space.dawdawich.repositories.redis.entity.AnalyzerMoneyModel
import space.dawdawich.strategy.strategies.GridTableStrategyRunner
import space.dawdawich.utils.calculatePercentageChange
import space.dawdawich.utils.getRightTopic
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours

@Service
class AnalyzerService(
        private val kafkaListenerContainerFactory: ConcurrentKafkaListenerContainerFactory<String, String>,
        private val symbolRepository: SymbolRepository,
        private val analyzerStabilityRepository: AnalyzerStabilityRepository,
        private val analyzerRepository: AnalyzerRepository,
        private val mongoTemplate: MongoTemplate,
) : ConsumerSeekAware {

    companion object {
        val comparator = Comparator<Pair<String, Double>> { p1, p2 -> p1.first.compareTo(p2.first) }
    }

    private val log = KotlinLogging.logger { }

    private val priceListeners = mutableMapOf<Pair<Int, Boolean>, PriceTickerListener>()
    private val partitionMap: MutableMap<String, Int> =
        mutableMapOf(*symbolRepository.findAll().map { it.symbol to it.partition }.toTypedArray())
    private val analyzers: MutableList<Analyzer> = mutableListOf()
    private val moneyUpdateQueue: ConcurrentSkipListSet<Pair<String, Double>> = ConcurrentSkipListSet(comparator)
    private val middlePriceUpdateQueue: ConcurrentSkipListSet<Pair<String, Double>> = ConcurrentSkipListSet(comparator)

    init {
        analyzerRepository.findAll().filter { it.isActive }.map { it.convert() }.forEach { addAnalyzer(it) }
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
        analyzerRepository.findByIdOrNull(analyzerId)?.let {
            if (it.isActive) {
                addAnalyzer(it.convert())
            }
        }
    }

    @KafkaListener(topics = [ACTIVATE_ANALYZERS_TOPIC])
    fun activateAnalyzers(analyzerIds: String) {
        runBlocking {
            analyzerRepository.findAllById(analyzerIds.split(",")).forEach {
                if (it.isActive) {
                    launch { addAnalyzer(it.convert()) }
                }
            }
        }
    }

    @KafkaListener(topics = [DELETE_ANALYZER_TOPIC])
    fun deleteAnalyzer(analyzerId: String) {
        analyzerRepository.deleteById(analyzerId)
        removeAnalyzer(analyzerId)
    }

    @KafkaListener(
        topics = [REQUEST_ANALYZER_RUNTIME_DATA],
        containerFactory = "kafkaListenerReplayingContainerFactory"
    )
    @SendTo(RESPONSE_ANALYZER_RUNTIME_DATA)
    fun requestAnalyzerRuntimeInfoData(analyzerId: String): AnalyzerRuntimeInfoModel? {
        return analyzers.find { analyzerId == it.id }?.let { analyzer ->
            val money = analyzer.getMoney()
            val stability = analyzerRepository.findByIdOrNull(analyzerId)?.stabilityCoef
                analyzer.getRuntimeInfo().position?.let { position ->
                    AnalyzerRuntimeInfoModel(
                        money,
                        stability,
                        if (position.long) "Buy" else "Sell",
                        position.entryPrice,
                        position.size
                    )
                } ?: AnalyzerRuntimeInfoModel(money, stability)
        }
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
    fun requestAnalyzer(request: RequestProfitableAnalyzer): StrategyConfigModel? {
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
    private fun updateMoneySnapshot() {
        val calculationStartTime = System.currentTimeMillis()
        if (analyzers.isNotEmpty()) {
            val copiedAnalyzers = analyzers.asSequence()
            runBlocking {
                copiedAnalyzers
                        .filter { it.getMoney() != it.previousSnapshotMoney }
                        .map {
                            it.previousSnapshotMoney = it.getMoney()
                            it.readyToUpdateStability = true
                            AnalyzerMoneyModel(it.id, it.getMoney())
                        }
                        .let { analyzerStabilityRepository.saveAll(it.toList()) }
            }
        }
        log.info { "Finish process update money snapshot. Time elapsed: ${System.currentTimeMillis() - calculationStartTime}" }
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES, initialDelay = 2)
    private fun calculateStabilityCoef() {
        if (analyzers.isNotEmpty()) {
            val ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, GridTableAnalyzerDocument::class.java)
            val calculationStartTime = System.currentTimeMillis()
            val copiedAnalyzers = analyzers.filter { it.readyToUpdateStability }.toList()
            runBlocking {
                copiedAnalyzers.forEach { analyzer ->
                    launch {
                        analyzerStabilityRepository.findAllByAnalyzerId(analyzer.id)
                                .sortedBy { it.timestamp }
                                .let { snapshots ->
                                    val update = Update()
                                    val now = System.currentTimeMillis()
                                    val oneOurBefore = now - 1.hours.inWholeMilliseconds
                                    val twelveOurBefore = now - 12.hours.inWholeMilliseconds
                                    val twentyFourOurBefore = now - 24.hours.inWholeMilliseconds
                                    analyzer.calculateStabilityCoef(snapshots.map { snap -> snap.money }).let {
                                        update.set("stabilityCoef", it)
                                    }
                                    snapshots.firstOrNull { it.timestamp > oneOurBefore }?.let { snapshot ->
                                        val pNl = snapshot.money.calculatePercentageChange(analyzer.getMoney()).toInt()
                                        update.set("pNl1", pNl)
                                    }
                                    snapshots.firstOrNull { it.timestamp > twelveOurBefore }?.let { snapshot ->
                                        val pNl = snapshot.money.calculatePercentageChange(analyzer.getMoney()).toInt()
                                        update.set("pNl12", pNl)
                                    }
                                    snapshots.firstOrNull { it.timestamp > twentyFourOurBefore }?.let { snapshot ->
                                        val pNl = snapshot.money.calculatePercentageChange(analyzer.getMoney()).toInt()
                                        update.set("pNl24", pNl)
                                    }

                                    ops.updateOne(
                                            Query.query(Criteria.where("_id").`is`(analyzer.id)),
                                            update
                                    )
                                }
                    }
                }
            }
            log.info { "Finish process analyzers stability. Time elapsed: ${System.currentTimeMillis() - calculationStartTime}" }
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

        priceListeners.getOrPut(partition to analyzer.demoAccount) {
            PriceTickerListener(
                kafkaListenerContainerFactory.createContainer(
                    TopicPartitionOffset(
                        getRightTopic(analyzer.market, analyzer.demoAccount),
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
            priceListeners[partition to it.demoAccount]?.removeObserver(it::acceptPriceChange)
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
            multiplier,
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
        market,
        demoAccount,
        id,
    )

    private fun getMostStableAnalyzerStrategyConfig(request: RequestProfitableAnalyzer): StrategyConfigModel? {
        val copiedAnalyzers = analyzers
            .asSequence()
            .filter { it.demoAccount == request.demoAccount }
            .filter { it.market == request.market }
            .filter { it.accountId == request.accountId }
            .toList()
        val maxStabilityCoef = copiedAnalyzers
            .asSequence()
            .filter { it.getMoney() > request.managerMoney }
            .maxBy { it.stabilityCoef }.stabilityCoef

        val mostStableAnalyzers = copiedAnalyzers.filter { it.stabilityCoef == maxStabilityCoef }

        return if (mostStableAnalyzers.none { it.id == request.currentAnalyzerId }) {
            mostStableAnalyzers
                .map { it.getStrategyConfig() }
                .filter { analyzerRepository.findByIdOrNull(it.id)!!.startCapital < it.money }
                .maxByOrNull { it.money }
        } else null
    }

    private fun getBiggestByMoneyAnalyzerStrategyConfig(request: RequestProfitableAnalyzer): StrategyConfigModel? {
        val copiedAnalyzers = analyzers
            .asSequence()
            .filter { it.demoAccount == request.demoAccount }
            .filter { it.market == request.market }
            .filter { it.accountId == request.accountId }
            .toList()
        val maxMoney = copiedAnalyzers.maxBy { analyzer -> analyzer.getMoney() }.getMoney()
        val mostProfitableAnalyzers = copiedAnalyzers.filter { it.getMoney() == maxMoney }
        if (mostProfitableAnalyzers.none { it.id == request.currentAnalyzerId }) {
            return mostProfitableAnalyzers
                .map { it.getStrategyConfig() }
                .filter { analyzerRepository.findByIdOrNull(it.id)!!.startCapital < it.money }
                .minByOrNull { it.multiplier }
        }
        return null
    }
}
