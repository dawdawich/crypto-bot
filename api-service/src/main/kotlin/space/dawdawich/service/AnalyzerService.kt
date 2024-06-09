package space.dawdawich.service

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.ACTIVATE_ANALYZERS_TOPIC
import space.dawdawich.constants.ACTIVATE_ANALYZER_TOPIC
import space.dawdawich.constants.DEACTIVATE_ANALYZER_TOPIC
import space.dawdawich.controller.model.analyzer.CreateAnalyzerBulkRequest
import space.dawdawich.controller.model.analyzer.CreateAnalyzerRequest
import space.dawdawich.controller.model.analyzer.GridTableAnalyzerResponse
import space.dawdawich.exception.AnalyzerLimitExceededException
import space.dawdawich.exception.model.AnalyzerNotFoundException
import space.dawdawich.integration.client.bybit.ByBitPublicHttpClient
import space.dawdawich.repositories.mongo.SymbolRepository
import space.dawdawich.repositories.mongo.entity.GridTableAnalyzerDocument
import space.dawdawich.model.constants.TradeStrategy
import space.dawdawich.repositories.mongo.AnalyzerRepository
import space.dawdawich.repositories.custom.model.AnalyzerFilter
import space.dawdawich.repositories.mongo.AccountTransactionRepository
import space.dawdawich.repositories.redis.AnalyzerStabilityRepository
import space.dawdawich.service.validation.AnalyzerValidationService
import space.dawdawich.utils.plusPercent
import java.util.*
import kotlin.jvm.Throws

/**
 * Service class that provides functionality for managing analyzers.
 */
@Service
class AnalyzerService(
    private val analyzerRepository: AnalyzerRepository,
    private val analyzerValidationService: AnalyzerValidationService,
    private val analyzerStabilityRepository: AnalyzerStabilityRepository,
    private val accountTransactionRepository: AccountTransactionRepository,
    private val symbolRepository: SymbolRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val publicBybitClient: ByBitPublicHttpClient,
    private val publicBybitTestClient: ByBitPublicHttpClient,
    private val folderService: FolderService,
) {

    /**
     * Retrieves the top 20 analyzers based on their percent difference between money and startCapital.
     *
     * @return A list of GridTableAnalyzerResponse objects representing the top analyzers.
     */
    fun getTopAnalyzers(): List<GridTableAnalyzerResponse> =
        analyzerRepository.findAllByPublic().sortedByDescending {
            val difference = it.money - it.startCapital
            val percentDifference = (difference / it.startCapital) * 100
            // The data is sorted by percent difference in descending order
            percentDifference
        }.take(20).map { GridTableAnalyzerResponse(it) }

    /**
     * Retrieves a list of GridTableAnalyzerResponse objects based on the provided parameters.
     *
     * @param accountId The account ID.
     * @param page The page number for pagination.
     * @param size The number of items per page.
     * @param status The status filter for analyzers. Can be null.
     * @param symbols The symbols filter for analyzers. Can be null.
     * @param fieldName The field name for sorting analyzers. Can be null.
     * @param orderDirection The order direction for sorting analyzers. Can be null.
     * @param folderId The folder ID filter for analyzers. Can be null.
     * @return A list of GridTableAnalyzerResponse objects.
     */
    fun getAnalyzers(
        accountId: String,
        page: Int,
        size: Int,
        status: Boolean?,
        symbols: String?,
        fieldName: String?,
        orderDirection: String?,
        folderId: String?,
    ): List<GridTableAnalyzerResponse> {
        val direction = if (orderDirection != null && orderDirection.equals(
                "asc",
                ignoreCase = true
            )
        ) Sort.Direction.ASC else Sort.Direction.DESC
        val sort = fieldName?.let { Sort.by(direction, it) }
        val analyzerIds = folderId?.let { folderService.getAnalyzersByFolderIdAndAccountId(accountId, it) }?.toList()

        return analyzerRepository.findAnalyzersFilteredAndSorted(
            accountId,
            analyzerIds,
            PageRequest.of(page, size),
            AnalyzerFilter(status, symbols?.split(",")?.toList() ?: emptyList()),
            sort
        )
            .map { analyzer ->
                if (analyzer.isActive) {
                    analyzerStabilityRepository.findFirstByAnalyzerIdOrderByTimestampDesc(analyzer.id)?.let { lastStability ->
                        analyzer.money = lastStability.money
                    }
                }
                GridTableAnalyzerResponse(analyzer)
            }.toList()
    }

    /**
     * Retrieves the counters for active, active and not active analyzers based on the provided parameters.
     *
     * @param accountId The account ID.
     * @param folderId The folder ID. Can be null.
     * @param symbols The list of symbols to filter analyzers.
     * @return A Triple object representing the counters of all, active and not active analyzers.
     */
    fun getAnalyzersCounters(accountId: String, folderId: String?, symbols: List<String>): Triple<Int, Int, Int> =
        folderId?.let {
            Triple(
                analyzerRepository.countActiveAnalyzersInFolder(it, null, symbols),
                analyzerRepository.countActiveAnalyzersInFolder(it, true, symbols),
                analyzerRepository.countActiveAnalyzersInFolder(it, false, symbols),
            )
        } ?: if (symbols.isNotEmpty()) {
            Triple(
                analyzerRepository.countByAccountIdAndSymbolInfoSymbolIn(accountId, symbols),
                analyzerRepository.countByAccountIdAndSymbolInfoSymbolInAndIsActive(accountId, symbols, true),
                analyzerRepository.countByAccountIdAndSymbolInfoSymbolInAndIsActive(accountId, symbols, false)
            )
        } else {
            Triple(
                analyzerRepository.countByAccountId(accountId),
                analyzerRepository.countByAccountIdAndIsActive(accountId),
                analyzerRepository.countByAccountIdAndIsActive(accountId, false)
            )
        }

    /**
     * Returns the number of active analyzers for the specified account.
     *
     * @param accountId The account ID.
     * @return The count of active analyzers.
     */
    fun getActiveAnalyzersCount(accountId: String) = analyzerRepository.countByAccountIdAndIsActive(accountId)

    /**
     * Updates the status of analyzers.
     *
     * @param accountId The account ID associated with the analyzers.
     * @param status The new status of the analyzers.
     * @param ids The list of analyzer IDs to update. If [all] is true, the list to exclude from processing.
     * @param all Flag indicating whether to update all analyzers or not. Default is false.
     */
    fun updateAnalyzersStatus(accountId: String, status: Boolean, ids: List<String>, all: Boolean = false): Unit =
        if (!all) {
            analyzerValidationService
                .validateAnalyzersExistByIdAndAccountId(ids, accountId)
                .let {
                    if (status) {
                        checkIsUserCanCreateAnalyzers(accountId, ids.size)
                    }
                }
                .let { processChangeStatus(ids, status) }
        } else {
            analyzerRepository.findAllByAccountIdAndIdNotIn(accountId, ids)
                .map { analyzer -> analyzer.id }
                .let { analyzerIds ->
                    if (status) {
                        checkIsUserCanCreateAnalyzers(accountId, analyzerIds.size)
                    }
                    analyzerRepository.setAnalyzersActiveStatus(analyzerIds, status)
                    analyzerIds.forEach { id ->
                        kafkaTemplate.send(if (status) ACTIVATE_ANALYZER_TOPIC else DEACTIVATE_ANALYZER_TOPIC, id)
                    }
                    processChangeStatus(analyzerIds, status)
                }
        }

    /**
     * Deletes analyzers based on the provided parameters.
     *
     * @param accountId The account ID associated with the analyzers.
     * @param ids The list of analyzer IDs to delete. If [all] is true, the list to exclude from processing.
     * @param all Flag indicating whether to delete all analyzers or not. Default is false.
     */
    fun deleteAnalyzers(accountId: String, ids: List<String>, all: Boolean = false): Unit =
        if (!all) {
            analyzerValidationService
                .validateAnalyzersExistByIdAndAccountId(ids, accountId)
                .let { analyzerRepository.deleteByIdIn(ids) }
                .let { folderService.removeAnalyzers(ids.toSet()) }
                .let { ids.forEach { id -> kafkaTemplate.send(DEACTIVATE_ANALYZER_TOPIC, id) } }
        } else {
            analyzerRepository.findAllByAccountIdAndIdNotIn(accountId, ids)
                .map { analyzers -> analyzers.id }
                .let { analyzerIds ->
                    analyzerRepository.deleteByIdIn(analyzerIds)
                    folderService.removeAnalyzers(analyzerIds.toSet())
                    analyzerIds.forEach { id -> kafkaTemplate.send(DEACTIVATE_ANALYZER_TOPIC, id) }
                }
        }

    /**
     * Retrieves a GridTableAnalyzerResponse object based on the provided analyzer ID and account ID.
     *
     * @param id The identifier of the analyzer.
     * @param accountId The account ID associated with the analyzer.
     * @return A GridTableAnalyzerResponse object representing the analyzer.
     * @throws AnalyzerNotFoundException if the analyzer with the given ID and account ID is not found.
     */
    fun getAnalyzer(id: String, accountId: String): GridTableAnalyzerResponse =
        GridTableAnalyzerResponse(
            analyzerRepository.findByIdAndAccountId(id, accountId)
                ?: throw AnalyzerNotFoundException("Analyzer '$id' is not found")
        )

    /**
     * Creates a new analyzer.
     *
     * @param accountId The ID of the account associated with the analyzer.
     * @param analyzerData The data for creating the analyzer.
     * @throws AnalyzerLimitExceededException if the maximum number of active analyzers is exceeded.
     */
    @Throws(AnalyzerLimitExceededException::class)
    fun createAnalyzer(accountId: String, analyzerData: CreateAnalyzerRequest) =
        analyzerData.apply {
            if (active) {
                checkIsUserCanCreateAnalyzers(accountId)
            }

            val analyzerId = UUID.randomUUID().toString()
            val gridTableAnalyzerDocument = GridTableAnalyzerDocument(
                analyzerId,
                accountId,
                public,
                diapason,
                gridSize,
                multiplier,
                stopLoss,
                takeProfit,
                symbolRepository.findByIdOrNull(symbol)!!,
                startCapital,
                active,
                demoAccount,
                market,
                strategy
            )
            analyzerRepository.insert(gridTableAnalyzerDocument)
            analyzerData.folders.forEach { folderId ->
                folderService.addAnalyzersToFolder(
                    accountId,
                    folderId,
                    setOf(analyzerId)
                )
            }
            if (active) {
                kafkaTemplate.send(ACTIVATE_ANALYZER_TOPIC, gridTableAnalyzerDocument.id)
            }
        }

    /**
     * Resets the analyzers with the given IDs.
     *
     * @param accountId The account ID associated with the analyzers.
     * @param ids The list of analyzer IDs to reset. If [all] is true, the list to exclude from processing.
     * @param all Flag indicating whether to reset all analyzers or not.
     * @throws AnalyzerNotFoundException if any of the analyzers are not found.
     */
    fun resetAnalyzers(accountId: String, ids: List<String>, all: Boolean) =
        if (!all) {
            analyzerValidationService
                .validateAnalyzersExistByIdAndAccountId(ids, accountId)
                .let { resetAnalyzers(ids) }
        } else {
            analyzerRepository.findAllByAccountIdAndIdNotIn(accountId, ids)
                .map { analyzer -> analyzer.id }
                .let { foundedIds -> resetAnalyzers(foundedIds) }
        }

    /**
     * Creates multiple analyzers in bulk for a given account.
     *
     * @param accountId The ID of the account associated with the analyzers.
     * @param request The request object containing the parameters for creating the analyzers.
     * @throws AnalyzerLimitExceededException if the maximum number of active analyzers is exceeded.
     *
     * @see [CreateAnalyzerRequest] for bulk create params
     */
    @Throws(AnalyzerLimitExceededException::class)
    @SuppressWarnings("kotlin:S3776")
    fun bulkCreate(accountId: String, request: CreateAnalyzerBulkRequest) {
        with(request) {

            if (active) {
                checkIsUserCanCreateAnalyzers(accountId, request.calculateSize())
            }

            val symbols = symbolRepository.findAllById(symbols)
            val analyzersToInsert = mutableListOf<GridTableAnalyzerDocument>()

            runBlocking {
                val pricesMap = (if (demoAccount) publicBybitTestClient else publicBybitClient).getPairCurrentPrice()
                for (symbol in symbols) {
                    launch {
//                        val currentPrice = runBlocking {
//                            (if (demoAccount) publicBybitTestClient else publicBybitClient).getPairCurrentPrice(symbol.symbol)
//                        }
//                        val instructions = runBlocking {
//                            (if (demoAccount) publicBybitTestClient else publicBybitClient).getPairInstructions(symbol.symbol)
//                        }
                        val currentPrice = pricesMap.first { it["symbol"] == symbol.symbol }["lastPrice"]!!.toDouble()
                        for (stopLoss in stopLossMin..stopLossMax step stopLossStep) {
                            for (takeProfit in takeProfitMin..takeProfitMax step takeProfitStep) {
                                for (diapason in diapasonMin..diapasonMax step diapasonStep) {
                                    for (gridSize in gridSizeMin..gridSizeMax step gridSizeStep) {
                                        for (multiplier in multiplierMin..multiplierMax step multiplierStep) {
                                            val startCapital = startCapital.toDouble()
                                            val moneyPerOrder = startCapital.plusPercent(-2) / gridSize
                                            val qty = moneyPerOrder * multiplier / currentPrice

                                            if (qty > symbol.minOrderQty/* && multiplier <= instructions.maxLeverage*/) {
                                                analyzersToInsert.add(
                                                    GridTableAnalyzerDocument(
                                                        UUID.randomUUID().toString(),
                                                        accountId,
                                                        public,
                                                        diapason,
                                                        gridSize,
                                                        multiplier,
                                                        stopLoss,
                                                        takeProfit,
                                                        symbol,
                                                        startCapital,
                                                        active,
                                                        demoAccount,
                                                        market,
                                                        TradeStrategy.GRID_TABLE_STRATEGY
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            analyzerRepository.insert(analyzersToInsert)
            folders.forEach {
                folderService.addAnalyzersToFolder(
                    accountId,
                    it,
                    analyzersToInsert.map { analyzer -> analyzer.id }.toSet()
                )
            }

            if (request.active) {
                kafkaTemplate.send(ACTIVATE_ANALYZERS_TOPIC, accountId)
            }
        }
    }

    /**
     * Processes the change of status for analyzers.
     *
     * @param ids The list of analyzer IDs to update.
     * @param status The new status of the analyzers.
     */
    private fun processChangeStatus(ids: List<String>, status: Boolean) {
        analyzerRepository.setAnalyzersActiveStatus(ids, status)
        ids.forEach { id ->
            kafkaTemplate.send(if (status) ACTIVATE_ANALYZER_TOPIC else DEACTIVATE_ANALYZER_TOPIC, id)
        }
    }

    /**
     * Resets the analyzers with the given IDs.
     *
     * @param ids The list of analyzer IDs to reset.
     * @throws AnalyzerNotFoundException if any of the analyzers are not found.
     */
    private fun resetAnalyzers(ids: List<String>) = let { analyzerRepository.setAnalyzersActiveStatus(ids, false) }
        .let { ids.forEach { id -> kafkaTemplate.send(DEACTIVATE_ANALYZER_TOPIC, id) } }
        .let { analyzerRepository.resetAnalyzers(ids) }
        .let { analyzerStabilityRepository.deleteByAnalyzerIdIn(ids) }
        .let { analyzerRepository.setAnalyzersActiveStatus(ids, true) }
        .let { ids.forEach { id -> kafkaTemplate.send(ACTIVATE_ANALYZER_TOPIC, id) } }

    /**
     * Checks if a user can create analyzers based on the account ID and the number of analyzers to create.
     *
     * @param accountId The ID of the account associated with the analyzers.
     * @param analyzersToCreate The number of analyzers to create. Default is 1.
     * @throws AnalyzerLimitExceededException Throws if the maximum number of active analyzers is exceeded.
     */
    private fun checkIsUserCanCreateAnalyzers(accountId: String, analyzersToCreate: Int = 1) {
        val activeAnalyzers = analyzerRepository.countByAccountIdAndIsActive(accountId)
        val activeSubs =
            accountTransactionRepository.getActiveTransactionsForTimeRange(accountId).sumOf { it.value } * 100

        if (activeAnalyzers + analyzersToCreate > activeSubs) {
            throw AnalyzerLimitExceededException()
        }
    }
}
