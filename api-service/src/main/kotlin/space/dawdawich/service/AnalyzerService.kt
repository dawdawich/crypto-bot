package space.dawdawich.service

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import space.dawdawich.constants.ACTIVATE_ANALYZERS_TOPIC
import space.dawdawich.constants.ACTIVATE_ANALYZER_TOPIC
import space.dawdawich.constants.DEACTIVATE_ANALYZER_TOPIC
import space.dawdawich.controller.model.analyzer.*
import space.dawdawich.exception.AnalyzerLimitExceededException
import space.dawdawich.exception.model.AnalyzerNotFoundException
import space.dawdawich.integration.client.bybit.ByBitPublicHttpClient
import space.dawdawich.repositories.custom.mongo.model.AnalyzerFilter
import space.dawdawich.repositories.mongo.AccountTransactionRepository
import space.dawdawich.repositories.mongo.AnalyzerRepository
import space.dawdawich.repositories.mongo.SymbolRepository
import space.dawdawich.repositories.mongo.entity.CandleTailStrategyAnalyzerDocument
import space.dawdawich.repositories.mongo.entity.GridTableAnalyzerDocument
import space.dawdawich.repositories.mongo.entity.RSIGridTableAnalyzerDocument
import space.dawdawich.repositories.redis.AnalyzerStabilityRepository
import space.dawdawich.service.validation.AnalyzerValidationService
import space.dawdawich.utils.plusPercent
import java.util.*

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
    private val rabbitTemplate: RabbitTemplate,
    private val publicBybitClient: ByBitPublicHttpClient,
    private val publicBybitTestClient: ByBitPublicHttpClient,
    private val folderService: FolderService,
    private val symbolService: SymbolService,
) {

    /**
     * Retrieves the top 20 analyzers based on their percent difference between money and startCapital.
     *
     * @return A list of GridTableAnalyzerResponse objects representing the top analyzers.
     */
    fun getTopAnalyzers(): List<AnalyzerResponse> =
        analyzerRepository.findAllByPublic().sortedByDescending {
            val difference = it.money - it.startCapital
            val percentDifference = (difference / it.startCapital) * 100
            // The data is sorted by percent difference in descending order
            percentDifference
        }
            .take(20)
            .map { AnalyzerResponse.fromDocument(it, symbolService.volatileCoefficients[it.symbolInfo.symbol]) }

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
    ): List<AnalyzerResponse> {
        val direction =
            if (orderDirection != null && orderDirection.equals("asc", ignoreCase = true)) Sort.Direction.ASC
            else Sort.Direction.DESC
        val sort = fieldName?.let { Sort.by(direction, it) }
        val analyzerIds = folderId?.let { folderService.getAnalyzersByFolderIdAndAccountId(accountId, it) }?.toList()

        return analyzerRepository.findAnalyzersFilteredAndSorted(
            accountId,
            analyzerIds,
            PageRequest.of(page, size),
            AnalyzerFilter(status, symbols?.split(",")?.toList() ?: emptyList()),
            sort
        ).map { analyzer ->
            AnalyzerResponse.fromDocument(
                analyzer,
                symbolService.volatileCoefficients[analyzer.symbolInfo.symbol]
            )
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
//                        checkIsUserCanCreateAnalyzers(accountId, ids.size)
                    }
                }
                .let { processChangeStatus(ids, status) }
        } else {
            analyzerRepository.findAllByAccountIdAndIdNotIn(accountId, ids)
                .map { analyzer -> analyzer.id }
                .let { analyzerIds ->
                    if (status) {
//                        checkIsUserCanCreateAnalyzers(accountId, analyzerIds.size)
                    }
                    analyzerRepository.setAnalyzersActiveStatus(analyzerIds, status)
                    analyzerIds.forEach { id ->
                        rabbitTemplate.convertAndSend(
                            if (status) ACTIVATE_ANALYZER_TOPIC else DEACTIVATE_ANALYZER_TOPIC,
                            id
                        )
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
                .let { ids.forEach { id -> rabbitTemplate.convertAndSend(DEACTIVATE_ANALYZER_TOPIC, id) } }
        } else {
            analyzerRepository.findAllByAccountIdAndIdNotIn(accountId, ids)
                .map { analyzers -> analyzers.id }
                .let { analyzerIds ->
                    analyzerRepository.deleteByIdIn(analyzerIds)
                    folderService.removeAnalyzers(analyzerIds.toSet())
                    analyzerIds.forEach { id -> rabbitTemplate.convertAndSend(DEACTIVATE_ANALYZER_TOPIC, id) }
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
    fun getAnalyzer(id: String, accountId: String): AnalyzerResponse {
        val analyzer = (analyzerRepository.findByIdAndAccountId(id, accountId)
            ?: throw AnalyzerNotFoundException("Analyzer '$id' is not found"))

        return when (analyzer) {
            is GridTableAnalyzerDocument -> GridTableAnalyzerResponse(
                analyzer,
                symbolService.volatileCoefficients[analyzer.symbolInfo.symbol]
            )

            is CandleTailStrategyAnalyzerDocument -> CandleTailAnalyzerResponse(
                analyzer,
                symbolService.volatileCoefficients[analyzer.symbolInfo.symbol]
            )

            is RSIGridTableAnalyzerDocument -> RSIGridTableAnalyzerResponse(
                analyzer,
                symbolService.volatileCoefficients[analyzer.symbolInfo.symbol]
            )
        }
    }

    /**
     * Creates a new analyzer.
     *
     * @param accountId The ID of the account associated with the analyzer.
     * @param analyzerData The data for creating the analyzer.
     * @throws AnalyzerLimitExceededException if the maximum number of active analyzers is exceeded.
     */
    @Throws(AnalyzerLimitExceededException::class)
    fun createAnalyzer(accountId: String, analyzerData: CreateAnalyzerRequest) =
        when (analyzerData) {
            is CreateGridAnalyzerRequest -> analyzerData.apply {
//                if (active) {
//                    checkIsUserCanCreateAnalyzers(accountId)
//                }

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
                    rabbitTemplate.convertAndSend(ACTIVATE_ANALYZER_TOPIC, gridTableAnalyzerDocument.id)
                }
            }

            is CreateCandleTailAnalyzerRequest -> analyzerData.apply {
//                if (active) {
//                    checkIsUserCanCreateAnalyzers(accountId)
//                }

                val analyzerId = UUID.randomUUID().toString()
                val analyzerDocument = CandleTailStrategyAnalyzerDocument(
                    analyzerId,
                    accountId,
                    public,
                    multiplier,
                    stopLoss,
                    takeProfit,
                    symbolRepository.findByIdOrNull(symbol)!!,
                    startCapital,
                    active,
                    demoAccount,
                    market,
                    kLineDuration,
                )
                analyzerRepository.insert(analyzerDocument)
                analyzerData.folders.forEach { folderId ->
                    folderService.addAnalyzersToFolder(
                        accountId,
                        folderId,
                        setOf(analyzerId)
                    )
                }
                if (active) {
                    rabbitTemplate.convertAndSend(ACTIVATE_ANALYZER_TOPIC, analyzerDocument.id)
                }
            }

            is CreateRSIGridAnalyzerRequest -> analyzerData.apply {
//                if (active) {
//                    checkIsUserCanCreateAnalyzers(accountId)
//                }

                val analyzerId = UUID.randomUUID().toString()
                val analyzerDocument = RSIGridTableAnalyzerDocument(
                    analyzerId,
                    accountId,
                    public,
                    gridSize,
                    multiplier,
                    stopLoss,
                    takeProfit,
                    symbolRepository.findByIdOrNull(symbol)!!,
                    startCapital,
                    active,
                    demoAccount,
                    market,
                    kLineDuration,
                )
                analyzerRepository.insert(analyzerDocument)
                analyzerData.folders.forEach { folderId ->
                    folderService.addAnalyzersToFolder(
                        accountId,
                        folderId,
                        setOf(analyzerId)
                    )
                }
                if (active) {
                    rabbitTemplate.convertAndSend(ACTIVATE_ANALYZER_TOPIC, analyzerDocument.id)
                }
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
     *
     * @see [CreateGridAnalyzerRequest] for bulk create params
     */
    @SuppressWarnings("kotlin:S3776")
    fun bulkCreate(accountId: String, request: CreateAnalyzerBulkRequest) {
        with(request) {
            val analyzersToInsert = when (request) {
                is CreateGridAnalyzerBulkRequest -> getGroupAnalyzers(accountId, request)
                is CreateCandleTailAnalyzerBulkRequest -> getGroupAnalyzers(accountId, request)
                is CreateRSIGridAnalyzerBulkRequest -> getGroupAnalyzers(accountId, request)
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
                rabbitTemplate.convertAndSend(ACTIVATE_ANALYZERS_TOPIC, accountId)
            }
        }
    }

    private fun getGroupAnalyzers(
        accountId: String,
        request: CreateGridAnalyzerBulkRequest,
    ): MutableList<GridTableAnalyzerDocument> =
        with(request) {
            val symbols = symbolRepository.findAllById(symbols)
            val analyzersToInsert = mutableListOf<GridTableAnalyzerDocument>()

            runBlocking {
                val pricesMap = (if (demoAccount) publicBybitTestClient else publicBybitClient).getPairCurrentPrice()
                for (symbol in symbols) {
                    launch {
                        val currentPrice = pricesMap.first { it["symbol"] == symbol.symbol }["lastPrice"]!!.toDouble()
                        for (stopLoss in stopLossMin..stopLossMax step stopLossStep) {
                            for (takeProfit in takeProfitMin..takeProfitMax step takeProfitStep) {
                                for (diapason in diapasonMin..diapasonMax step diapasonStep) {
                                    for (gridSize in gridSizeMin..gridSizeMax step gridSizeStep) {
                                        for (multiplier in multiplierMin..multiplierMax step multiplierStep) {
                                            val startCapital = startCapital.toDouble()
                                            val moneyPerOrder = startCapital.plusPercent(-2) / gridSize
                                            val qty = moneyPerOrder * multiplier / currentPrice

                                            if (qty > symbol.minOrderQty && multiplier <= symbol.maxLeverage) {
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
                                                        market
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
            return analyzersToInsert
        }

    private fun getGroupAnalyzers(
        accountId: String,
        request: CreateCandleTailAnalyzerBulkRequest,
    ): MutableList<CandleTailStrategyAnalyzerDocument> =
        with(request) {
            val symbols = symbolRepository.findAllById(symbols)
            val analyzersToInsert = mutableListOf<CandleTailStrategyAnalyzerDocument>()

            runBlocking {
                for (symbol in symbols) {
                    launch {
                        for (stopLoss in stopLossMin..stopLossMax step stopLossStep) {
                            for (takeProfit in takeProfitMin..takeProfitMax step takeProfitStep) {
                                for (multiplier in multiplierMin..multiplierMax step multiplierStep) {
                                    for (kLineDuration in kLineDurations) {
                                        if (multiplier <= symbol.maxLeverage) {
                                            analyzersToInsert.add(
                                                CandleTailStrategyAnalyzerDocument(
                                                    UUID.randomUUID().toString(),
                                                    accountId,
                                                    public,
                                                    multiplier,
                                                    stopLoss,
                                                    takeProfit,
                                                    symbol,
                                                    startCapital.toDouble(),
                                                    active,
                                                    demoAccount,
                                                    market,
                                                    kLineDuration
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

            return analyzersToInsert
        }

    private fun getGroupAnalyzers(
        accountId: String,
        request: CreateRSIGridAnalyzerBulkRequest,
    ): MutableList<RSIGridTableAnalyzerDocument> =
        with(request) {
            val symbols = symbolRepository.findAllById(symbols)
            val analyzersToInsert = mutableListOf<RSIGridTableAnalyzerDocument>()

            runBlocking {
                for (symbol in symbols) {
                    launch {
                        for (stopLoss in stopLossMin..stopLossMax step stopLossStep) {
                            for (takeProfit in takeProfitMin..takeProfitMax step takeProfitStep) {
                                for (multiplier in multiplierMin..multiplierMax step multiplierStep) {
                                    for (kLineDuration in kLineDurations) {
                                        for (gridSize in gridSizeMin..gridSizeMax step gridSizeStep) {
                                            if (multiplier <= symbol.maxLeverage) {
                                                analyzersToInsert.add(
                                                    RSIGridTableAnalyzerDocument(
                                                        UUID.randomUUID().toString(),
                                                        accountId,
                                                        public,
                                                        gridSize,
                                                        multiplier,
                                                        stopLoss,
                                                        takeProfit,
                                                        symbol,
                                                        startCapital.toDouble(),
                                                        active,
                                                        demoAccount,
                                                        market,
                                                        kLineDuration
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

            return analyzersToInsert
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
            rabbitTemplate.convertAndSend(if (status) ACTIVATE_ANALYZER_TOPIC else DEACTIVATE_ANALYZER_TOPIC, id)
        }
    }

    /**
     * Resets the analyzers with the given IDs.
     *
     * @param ids The list of analyzer IDs to reset.
     * @throws AnalyzerNotFoundException if any of the analyzers are not found.
     */
    private fun resetAnalyzers(ids: List<String>) = let { analyzerRepository.setAnalyzersActiveStatus(ids, false) }
        .let { ids.forEach { id -> rabbitTemplate.convertAndSend(DEACTIVATE_ANALYZER_TOPIC, id) } }
        .let { analyzerRepository.resetAnalyzers(ids) }
        .let {
            runBlocking {
                ids.forEach { analyzerId ->
                    launch {
                        analyzerStabilityRepository.findAllByAnalyzerId(analyzerId).forEach { model ->
                            analyzerStabilityRepository.deleteById(model.id)
                        }
                    }
                }
            }
        }
        .let { analyzerRepository.setAnalyzersActiveStatus(ids, true) }
        .let { ids.forEach { id -> rabbitTemplate.convertAndSend(ACTIVATE_ANALYZER_TOPIC, id) } }

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
