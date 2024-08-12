package space.dawdawich.services

import com.fasterxml.jackson.core.type.TypeReference
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import space.dawdawich.analyzers.Analyzer
import space.dawdawich.analyzers.KLineStrategyAnalyzer
import space.dawdawich.constants.*
import space.dawdawich.model.RequestProfitableAnalyzer
import space.dawdawich.model.analyzer.KLineRecord
import space.dawdawich.model.constants.AnalyzerChooseStrategy
import space.dawdawich.model.strategy.AnalyzerRuntimeInfoModel
import space.dawdawich.model.strategy.KLineStrategyConfigModel
import space.dawdawich.model.strategy.StrategyConfigModel
import space.dawdawich.repositories.mongo.AnalyzerRepository
import space.dawdawich.repositories.mongo.entity.AnalyzerDocument
import space.dawdawich.repositories.mongo.entity.CandleTailStrategyAnalyzerDocument
import space.dawdawich.repositories.mongo.entity.GridTableAnalyzerDocument
import space.dawdawich.repositories.mongo.entity.RSIGridTableAnalyzerDocument
import space.dawdawich.repositories.redis.AnalyzerStabilityRepository
import space.dawdawich.repositories.redis.entity.AnalyzerMoneyModel
import space.dawdawich.utils.calculatePercentageChange
import space.dawdawich.utils.convert
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Service
class AnalyzerService(
    private val connectionFactory: ConnectionFactory,
    private val analyzerStabilityRepository: AnalyzerStabilityRepository,
    private val analyzerRepository: AnalyzerRepository,
    private val mongoTemplate: MongoTemplate,
) {

    companion object {
        val comparator = Comparator<Pair<String, Double>> { p1, p2 -> p1.first.compareTo(p2.first) }
    }

    private val log = KotlinLogging.logger { }

    private val priceListeners = mutableMapOf<Pair<String, Boolean>, EventListener<Double>>()
    private val kLineListeners = mutableMapOf<Pair<String, Int>, EventListener<KLineRecord>>() // TODO: Add separator for demo acs
    private val analyzers: MutableList<Analyzer> = mutableListOf()
    private val moneyUpdateQueue: ConcurrentSkipListSet<Pair<String, Double>> = ConcurrentSkipListSet(comparator)
    private val middlePriceUpdateQueue: ConcurrentSkipListSet<Pair<String, Double>> = ConcurrentSkipListSet(comparator)

    init {
        analyzerRepository.findAll().filter { it.isActive }.map { it.convert() }.forEach { addAnalyzer(it) }
    }

    @RabbitListener(queues = [DEACTIVATE_ANALYZER_TOPIC])
    fun deactivateAnalyzer(analyzerId: String) {
        removeAnalyzer(analyzerId)
    }

    @RabbitListener(queues = [ACTIVATE_ANALYZER_TOPIC])
    fun activateAnalyzer(analyzerId: String) {
        analyzerRepository.findByIdOrNull(analyzerId)?.let {
            if (it.isActive) {
                addAnalyzer(it.convert())
            }
        }
    }

    @RabbitListener(queues = [ACTIVATE_ANALYZERS_TOPIC])
    fun activateAnalyzers(accountId: String) {
        runBlocking {
            analyzerRepository.findAllByAccountIdAndPublic(accountId, true).forEach { doc ->
                if (doc.isActive && analyzers.none { analyzer -> analyzer.id == doc.id }) {
                    launch { addAnalyzer(doc.convert()) }
                }
            }
        }
    }

    @RabbitListener(queues = [DELETE_ANALYZER_TOPIC])
    fun deleteAnalyzer(analyzerId: String) {
        analyzerRepository.deleteById(analyzerId)
        removeAnalyzer(analyzerId)
    }

    @RabbitListener(queues = [REQUEST_ANALYZER_RUNTIME_DATA])
//    @SendTo(RESPONSE_ANALYZER_RUNTIME_DATA)
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

    @RabbitListener(queues = [REQUEST_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC])
    fun requestAnalyzerData(analyzerId: String) =
        analyzers.find { analyzerId == it.id }?.getRuntimeInfo()

    @RabbitListener(queues = [REQUEST_PROFITABLE_ANALYZER_STRATEGY_CONFIG_TOPIC])
    fun requestAnalyzer(request: RequestProfitableAnalyzer): StrategyConfigModel? {
        return when (request.chooseStrategy) { // TODO: add filtering by folder
            AnalyzerChooseStrategy.MOST_STABLE -> getMostStableAnalyzerStrategyConfig(request)
            AnalyzerChooseStrategy.BIGGEST_BY_MONEY -> getBiggestByMoneyAnalyzerStrategyConfig(request)
            AnalyzerChooseStrategy.TEST -> getFilteredAnalyzerConfig(request)
            AnalyzerChooseStrategy.CUSTOM -> getMostProfitableForLast10MinutesAnalyzerStrategyConfig(request)
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
            copiedAnalyzers
                .filter { it.getMoney() != it.previousSnapshotMoney }
                .map {
                    it.previousSnapshotMoney = it.getMoney()
                    it.readyToUpdateStability = true
                    AnalyzerMoneyModel(it.id, it.getMoney())
                }
                .let {
                    val list = it.toList()
                    if (list.isNotEmpty()) {
                        analyzerStabilityRepository.saveAll(list)
                    }
                }
        }
        log.info { "Finish process update money snapshot. Time elapsed: ${System.currentTimeMillis() - calculationStartTime}" }
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES, initialDelay = 2)
    private fun calculateStabilityCoef() {
        if (analyzers.isNotEmpty()) {
            var isOpsEmpty = true
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
                                val tenMinutesBefore = now - 10.minutes.inWholeMilliseconds
                                val oneOurBefore = now - 1.hours.inWholeMilliseconds
                                val twelveOurBefore = now - 12.hours.inWholeMilliseconds
                                val twentyFourOurBefore = now - 24.hours.inWholeMilliseconds
                                analyzer.calculateStabilityCoef(snapshots.map { snap -> snap.money }).let {
                                    update["stabilityCoef"] = it
                                }
                                snapshots.firstOrNull { it.timestamp > tenMinutesBefore }?.let { snapshot ->
                                    val pNl = snapshot.money.calculatePercentageChange(analyzer.getMoney()).toInt()
                                    update["pNl10M"] = pNl
                                }
                                snapshots.firstOrNull { it.timestamp > oneOurBefore }?.let { snapshot ->
                                    val pNl = snapshot.money.calculatePercentageChange(analyzer.getMoney()).toInt()
                                    update["pNl1"] = pNl
                                }
                                snapshots.firstOrNull { it.timestamp > twelveOurBefore }?.let { snapshot ->
                                    val pNl = snapshot.money.calculatePercentageChange(analyzer.getMoney()).toInt()
                                    update["pNl12"] = pNl
                                }
                                snapshots.firstOrNull { it.timestamp > twentyFourOurBefore }?.let { snapshot ->
                                    val pNl = snapshot.money.calculatePercentageChange(analyzer.getMoney()).toInt()
                                    update["pNl24"] = pNl
                                }

                                ops.updateOne(
                                    Query.query(Criteria.where("_id").`is`(analyzer.id)),
                                    update
                                )
                                isOpsEmpty = false
                            }
                    }
                }
            }
            log.info { "Finish process analyzers stability. Time elapsed: ${System.currentTimeMillis() - calculationStartTime}" }
            if (!isOpsEmpty) {
                ops.execute()
            }
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
        priceListeners.getOrPut(analyzer.symbol to analyzer.demoAccount) {
            EventListener(connectionFactory, if (analyzer.demoAccount) BYBIT_TEST_TICKER_TOPIC else BYBIT_TICKER_TOPIC, analyzer.symbol, object : TypeReference<Double>() {})
        }.addObserver(analyzer::acceptPriceChange)
        if (analyzer is KLineStrategyAnalyzer) {
            val kLineDuration = (analyzer.getStrategyConfig() as KLineStrategyConfigModel).kLineDuration
            kLineListeners.getOrPut(analyzer.symbol to kLineDuration) {
                EventListener(connectionFactory, if (analyzer.demoAccount) BYBIT_TEST_KLINE_TOPIC else BYBIT_KLINE_TOPIC, "$kLineDuration.${analyzer.symbol}", object : TypeReference<KLineRecord>() {})
            }.addObserver(analyzer::acceptCandle)
        }

        analyzers += analyzer
    }

    private fun removeAnalyzer(analyzerId: String) {
        analyzers.find { it.id == analyzerId }?.let {
            priceListeners[it.symbol to it.demoAccount]?.removeObserver(it::acceptPriceChange)
            if (it is KLineStrategyAnalyzer) {
                kLineListeners[it.symbol to (it.getStrategyConfig() as KLineStrategyConfigModel).kLineDuration]?.removeObserver(it::acceptCandle)
            }
        }

        analyzers.removeIf { it.id == analyzerId }
    }

    private fun AnalyzerDocument.convert() = when (this) {
        is GridTableAnalyzerDocument -> convert({ _, newValue -> moneyUpdateQueue += id to newValue }, { middlePrice -> middlePriceUpdateQueue += id to middlePrice },)
        is CandleTailStrategyAnalyzerDocument -> convert { _, newValue -> moneyUpdateQueue += id to newValue }
        is RSIGridTableAnalyzerDocument -> convert { _, newValue -> moneyUpdateQueue += id to newValue }
    }

    private fun getMostStableAnalyzerStrategyConfig(request: RequestProfitableAnalyzer): StrategyConfigModel? {
        log.info { "Try to find analyzer for account id '${request.accountId}' by stability" }
        val copiedAnalyzers = analyzers
            .asSequence()
            .filter { it.demoAccount == request.demoAccount }
            .filter { it.market == request.market }
            .filter { it.accountId == request.accountId }
            .toList()
        val maxStabilityCoef = copiedAnalyzers
            .asSequence()
            .filter { it.getMoney() > request.managerMoney }
            .maxByOrNull { it.stabilityCoef }?.stabilityCoef

        val mostStableAnalyzers = copiedAnalyzers.filter { it.stabilityCoef == maxStabilityCoef }

        return if (mostStableAnalyzers.none { it.id == request.currentAnalyzerId }) {
            mostStableAnalyzers
                .filter { it.startCapital < it.getStrategyConfig().money }
                .maxByOrNull { it.getMoney() }?.getStrategyConfig()
        } else null
    }

    private fun getMostProfitableForLast10MinutesAnalyzerStrategyConfig(request: RequestProfitableAnalyzer): StrategyConfigModel? {
        log.info { "Try to find analyzer for account id '${request.accountId}' by profit for last 10 minutes" }
        val moreSuitableAnalyzer = analyzerRepository.findMoreProfitableByLast10Minutes(request.accountId, request.demoAccount, request.market.name)

        return if (moreSuitableAnalyzer != null && moreSuitableAnalyzer.none { it.id != request.currentAnalyzerId}) {
            analyzers.firstOrNull { it.id == moreSuitableAnalyzer.filter { it.pNl10M != null }.maxByOrNull { it.pNl10M!! }?.id }?.getStrategyConfig()
        } else null
    }

    private fun getBiggestByMoneyAnalyzerStrategyConfig(request: RequestProfitableAnalyzer): StrategyConfigModel? {
        log.info { "Try to find analyzer for account id '${request.accountId}' by money" }
        val copiedAnalyzers = analyzers
            .asSequence()
            .filter { it.demoAccount == request.demoAccount }
            .filter { it.market == request.market }
            .filter { it.accountId == request.accountId }
            .toList()
        val maxMoney = copiedAnalyzers.maxByOrNull { analyzer -> analyzer.getMoney() }?.getMoney()
        val mostProfitableAnalyzers = copiedAnalyzers.filter { it.getMoney() == maxMoney }
        if (mostProfitableAnalyzers.none { it.id == request.currentAnalyzerId }) {
            return mostProfitableAnalyzers
                .map { it.getStrategyConfig() }
                .filter { analyzerRepository.findByIdOrNull(it.id)!!.startCapital < it.money }
                .minByOrNull { it.multiplier }
        }
        return null
    }

    private fun getFilteredAnalyzerConfig(request: RequestProfitableAnalyzer): StrategyConfigModel? {
        val analyzersDocs =
            analyzerRepository.findAllById(analyzers
                .asSequence()
                .filter { it.demoAccount == request.demoAccount }
                .filter { it.market == request.market }
                .filter { it.accountId == request.accountId }
                .map { it.id }
                .toList())
        val neededAnalyzer = analyzersDocs
            .asSequence()
            .filter { (it.stabilityCoef ?: 0.0) >= 2 } // sortedStabilityHigherThanFour
            .sortedByDescending { it.pNl24 }
            .take(40) // take 40 analyzers by pnl24
            .sortedByDescending { it.pNl12 }
            .take(20) // take 20 analyzers by pnl12
            .sortedByDescending { it.pNl1 }
            .take(10) // take 10 analyzers by pnl12
            .maxByOrNull { it.money }?.id

        return neededAnalyzer?.let { analyzers.first { analyzer -> analyzer.id == it }.getStrategyConfig() }
    }
}
