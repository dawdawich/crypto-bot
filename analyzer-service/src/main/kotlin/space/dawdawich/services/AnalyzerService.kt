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

/**
 * Service class responsible for handling and managing analyzers and their associated operations.
 * Provides functionality for activating, deactivating, deleting, and retrieving runtime data of analyzers.
 *
 * This service interacts with message queues to respond to events related to analyzers,
 * and schedules tasks to process updates and calculate statistics for analyzer stability and performance.
 *
 * Dependencies:
 * - `ConnectionFactory`: Used for establishing connections to messaging queues for event listeners.
 * - `AnalyzerStabilityRepository`: Repository for storing stability-related data of analyzers.
 * - `AnalyzerRepository`: Repository for CRUD operations on analyzers.
 * - `MongoTemplate`: MongoDB template for advanced database operations (bulk updates).
 *
 * Key Tasks:
 * - Manages a list of active analyzers and their associated listeners.
 * - Processes incoming messages from RabbitMQ topics to perform operations like activation, deactivation,
 *   deletion, and strategy configuration retrieval.
 * - Periodically schedules tasks to update middle price lists, money snapshots, and calculate stability coefficients.
 *
 * Initialization:
 * - Initializes active analyzers from the database upon service startup.
 *
 * Scheduled Operations:
 * - `processMiddlePriceUpdateList`: Updates middle prices and money values from a queue every 30 seconds.
 * - `updateMoneySnapshot`: Updates snapshots of money values for analyzers every 1 minute.
 * - `calculateStabilityCoef`: Calculates statistical stability coefficients for analyzers every 1 minute.
 *
 * RabbitMQ Listeners:
 * - Listens and handles messages related to activating, deactivating, deleting analyzers, and retrieving runtime data.
 * - Topics include:
 *   - `DEACTIVATE_ANALYZER_TOPIC`: Deactivates an analyzer.
 *   - `ACTIVATE_ANALYZER_TOPIC`: Activates an analyzer.
 *   - `ACTIVATE_ANALYZERS_TOPIC`: Activates all analyzers for a specific account.
 *   - `DELETE_ANALYZER_TOPIC`: Deletes an analyzer.
 *   - `REQUEST_ANALYZER_RUNTIME_DATA`: Retrieves runtime info for a specific analyzer.
 *   - `REQUEST_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC`: Retrieves strategy runtime data.
 *   - `REQUEST_PROFITABLE_ANALYZER_STRATEGY_CONFIG_TOPIC`: Retrieves the most profitable analyzer strategy configuration.
 */
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

    private val priceListeners = mutableMapOf<Pair<String, Boolean>, EventListener<Double>>() // Price change event listeners
    private val kLineListeners = mutableMapOf<Pair<String, Int>, EventListener<KLineRecord>>() // KLine event listener
    private val analyzers: MutableList<Analyzer> = mutableListOf() // Active analyzers
    private val moneyUpdateQueue: ConcurrentSkipListSet<Pair<String, Double>> = ConcurrentSkipListSet(comparator) // List with analyzers to update
    private val middlePriceUpdateQueue: ConcurrentSkipListSet<Pair<String, Double>> = ConcurrentSkipListSet(comparator) // List with analyzers to update

    init {
        analyzerRepository.findAll().filter { it.isActive }.map { it.convert() }.forEach { addAnalyzer(it) }
    }

    /**
     * Deactivates an analyzer based on its unique identifier.
     * Removes the analyzer from the internal list and disables any observers or listeners
     * associated with its functionality.
     *
     * @param analyzerId The unique identifier of the analyzer to be deactivated.
     */
    @RabbitListener(queues = [DEACTIVATE_ANALYZER_TOPIC])
    fun deactivateAnalyzer(analyzerId: String) {
        removeAnalyzer(analyzerId)
    }

    /**
     * Activates an analyzer based on its unique identifier. If the analyzer is found and is marked as active,
     * it is added to the system for further data processing and monitoring.
     *
     * @param analyzerId The unique identifier of the analyzer to be activated.
     */
    @RabbitListener(queues = [ACTIVATE_ANALYZER_TOPIC])
    fun activateAnalyzer(analyzerId: String) {
        analyzerRepository.findByIdOrNull(analyzerId)?.let {
            if (it.isActive) {
                addAnalyzer(it.convert())
            }
        }
    }

    /**
     * Activates all public and active analyzers for a given account.
     *
     * This function retrieves all analyzers associated with the specified account ID
     * that are marked as public and active. If an analyzer is not already present in
     * the system, it is added and initialized for monitoring.
     *
     * @param accountId The unique identifier of the account for which analyzers need to be activated.
     */
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

    /**
     * Deletes an analyzer based on its unique identifier.
     *
     * This method removes the analyzer from the database and
     * performs a cleanup by invoking the necessary removal logic
     * to ensure proper disconnection from the system.
     *
     * @param analyzerId The unique identifier of the analyzer to be deleted.
     */
    @RabbitListener(queues = [DELETE_ANALYZER_TOPIC])
    fun deleteAnalyzer(analyzerId: String) {
        analyzerRepository.deleteById(analyzerId)
        removeAnalyzer(analyzerId)
    }

    /**
     * Retrieves runtime information for a specific analyzer identified by its unique ID.
     * The method collects data such as the analyzer's financial status, stability coefficient,
     * and position details if available. This data is used to construct an
     * `AnalyzerRuntimeInfoModel`, which represents the runtime state of the analyzer.
     *
     * @param analyzerId The unique identifier of the analyzer whose runtime info is to be retrieved.
     * @return An `AnalyzerRuntimeInfoModel` containing the runtime state of the analyzer,
     * or null if the analyzer is not found or no runtime info is available.
     */
    @RabbitListener(queues = [REQUEST_ANALYZER_RUNTIME_DATA])
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

    /**
     * Retrieves runtime data for a specific analyzer identified by its unique ID.
     * This method searches the list of analyzers and, if a match is found, retrieves
     * its runtime strategy data using the `getRuntimeInfo` method.
     *
     * @param analyzerId The unique identifier of the analyzer whose strategy runtime data is to be retrieved.
     */
    @RabbitListener(queues = [REQUEST_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC])
    fun requestAnalyzerData(analyzerId: String) =
        analyzers.find { analyzerId == it.id }?.getRuntimeInfo()

    /**
     * Analyzes the given request and returns a strategy configuration model based on the chosen strategy.
     *
     * The method processes the given request by determining which strategy configuration
     * should be used based on the AnalyzerChooseStrategy specified in the request.
     *
     * @param request the request object containing the strategy choice and necessary details for analysis
     * @return a StrategyConfigModel based on the chosen analysis strategy;
     *         null if no strategy configuration is applicable
     */
    @RabbitListener(queues = [REQUEST_PROFITABLE_ANALYZER_STRATEGY_CONFIG_TOPIC])
    fun requestAnalyzer(request: RequestProfitableAnalyzer): StrategyConfigModel? {
        return when (request.chooseStrategy) { // TODO: add filtering by folder
            AnalyzerChooseStrategy.MOST_STABLE -> getMostStableAnalyzerStrategyConfig(request)
            AnalyzerChooseStrategy.BIGGEST_BY_MONEY -> getBiggestByMoneyAnalyzerStrategyConfig(request)
            AnalyzerChooseStrategy.TEST -> getFilteredAnalyzerConfig(request)
            AnalyzerChooseStrategy.CUSTOM -> getMostProfitableForLast10MinutesAnalyzerStrategyConfig(request)
        }
    }

    /**
     * Periodically processes and updates the middle price and money information in the respective queues. This
     * information represents a runtime data of analyzers which should be updated periodically.
     *
     * This method checks if the `middlePriceUpdateQueue` and `moneyUpdateQueue` contain items.
     * If the queues are not empty, the method delegates the processing of their contents to the
     * `updateList` method, using "middlePrice" or "money" as the field name for updates, respectively.
     * It is scheduled to execute every 30 seconds.
     */
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    private fun processMiddlePriceUpdateList() {
        if (middlePriceUpdateQueue.isNotEmpty()) {
            updateList(middlePriceUpdateQueue, "middlePrice")
        }
        if (moneyUpdateQueue.isNotEmpty()) {
            updateList(moneyUpdateQueue, "money")
        }
    }

    /**
     * Periodically updates the money snapshot of analyzers and saves changes to the repository.
     *
     * This method iterates over all available analyzers and compares the current money value obtained via
     * the `getMoney` method with their previously recorded snapshot. If the money value has changed, the
     * analyzer's snapshot is updated, and the `readyToUpdateStability` flag of the analyzer is set to `true`.
     * The updated values are then persisted in the `analyzerStabilityRepository`.
     *
     * The process executes every minute, as defined by the `@Scheduled` annotation.
     *
     * Key steps:
     * 1. Copies the list of analyzers to avoid concurrent modification issues.
     * 2. Filters analyzers where the current money value has changed compared to their previous snapshot.
     * 3. Maps the updated analyzers to a list of `AnalyzerMoneyModel` instances.
     * 4. Saves all updated snapshots to the repository if changes are detected.
     * 5. Logs the time taken to complete the update process.
     */
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    private fun updateMoneySnapshot() {
        val calculationStartTime = System.currentTimeMillis()
        if (analyzers.isNotEmpty()) {
            analyzers.asSequence()
                .filter { it.getMoney() != it.previousSnapshotMoney } // need to detect if analyzer capital changed
                .map {
                    it.previousSnapshotMoney = it.getMoney()
                    AnalyzerMoneyModel(it.id, it.getMoney())
                }
                .let {
                    it.toList().let { list ->
                        if (list.isNotEmpty()) {
                            analyzerStabilityRepository.saveAll(list)
                        }
                    }
                }
        }
        log.info { "Finish process update money snapshot. Time elapsed: ${System.currentTimeMillis() - calculationStartTime}" }
    }

    /**
     * A scheduled function responsible for calculating and updating the stability coefficient and
     * related performance metrics for a set of analyzers. The function processes the data in batches
     * and updates the corresponding records in the database.
     *
     * The method uses asynchronous operations to handle multiple analyzers concurrently.
     * Calculations include the stability coefficient and percentage changes in financial metrics (pNL)
     * for various time intervals such as 10 minutes, 1 hour, 12 hours, and 24 hours.
     *
     * Function Behavior:
     * - Executes with a fixed delay of 1 minute and an initial delay of 2 minutes.
     * - Filters analyzers ready for stability updates and processes each analyzer asynchronously.
     * - For each analyzer, historical snapshots of stability data are retrieved and sorted by timestamp.
     * - The stability coefficient and performance metrics are calculated and updated using the snapshots.
     * - Updates are applied using bulk operations to optimize database performance.
     */
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

    /**
     * Updates a specific field for a batch of records in the database using bulk operations.
     *
     * This method processes a thread-safe concurrent list of key-value pairs where each key
     * represents the unique identifier of a record, and the value corresponds to the new value
     * to set for the specified field. Records are updated in an unordered bulk operation, which
     * optimizes performance for batch updates.
     *
     * @param updateList A thread-safe concurrent set containing pairs of record IDs (as strings)
     *                   and their corresponding new field values (as doubles). The method processes
     *                   and removes items from this list one by one.
     * @param fieldName  The name of the field to be updated in the database for each record.
     */
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

    /**
     * Adds an analyzer to the system and initializes observers and listeners for the analyzer's data processing.
     *
     * Depending on the type and configuration of the analyzer, it registers the analyzer to observe price and/or
     * K-line data updates. Observers are set up to trigger appropriate methods on the analyzer when data changes occur.
     *
     * @param analyzer The analyzer instance to be added. The analyzer contains configuration details such as the symbol,
     *                 account type (demo or real), and strategy specifics. If the analyzer is a K-line strategy analyzer,
     *                 an additional event listener for K-line updates is also initialized.
     */
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

    /**
     * Removes an analyzer identified by its unique ID from the system.
     *
     * This method performs the following steps:
     * - Searches for the analyzer in the list of currently active analyzers.
     * - If found, removes associated observers and listeners tied to the analyzer's price and K-line updates.
     * - Finally, removes the analyzer itself from the analyzers list.
     *
     * @param analyzerId The unique identifier of the analyzer to be removed.
     */
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

        return if (moreSuitableAnalyzer.none { it.id == request.currentAnalyzerId}) {
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
