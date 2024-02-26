package space.dawdawich.service

import kotlinx.coroutines.runBlocking
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.ACTIVATE_ANALYZERS_TOPIC
import space.dawdawich.constants.ACTIVATE_ANALYZER_TOPIC
import space.dawdawich.constants.DEACTIVATE_ANALYZER_TOPIC
import space.dawdawich.controller.model.CreateAnalyzerBulkRequest
import space.dawdawich.controller.model.CreateAnalyzerRequest
import space.dawdawich.controller.model.GridTableAnalyzerResponse
import space.dawdawich.exception.model.AnalyzerNotFoundException
import space.dawdawich.integration.client.bybit.ByBitPublicHttpClient
import space.dawdawich.model.constants.TradeStrategy
import space.dawdawich.repositories.AnalyzerRepository
import space.dawdawich.repositories.SymbolRepository
import space.dawdawich.repositories.custom.model.AnalyzerFilter
import space.dawdawich.repositories.entity.GridTableAnalyzerDocument
import space.dawdawich.service.validation.AnalyzerValidationService
import space.dawdawich.utils.plusPercent
import java.util.*

@Service
class AnalyzerService(
    private val analyzerRepository: AnalyzerRepository,
    private val analyzerValidationService: AnalyzerValidationService,
    private val symbolRepository: SymbolRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val publicBybitClient: ByBitPublicHttpClient,
    private val publicBybitTestClient: ByBitPublicHttpClient,
    private val folderService: FolderService,
) {

    fun getTopAnalyzers(): List<GridTableAnalyzerResponse> =
        analyzerRepository.findAllByPublic().sortedByDescending {
            val difference = it.money - it.startCapital
            val percentDifference = (difference / it.startCapital) * 100
            // The data is sorted by percent difference in descending order
            percentDifference
        }.map { GridTableAnalyzerResponse(it) }.take(20)

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
            .map { GridTableAnalyzerResponse(it) }.toList()
    }

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

    fun updateAnalyzersStatus(accountId: String, ids: List<String>, status: Boolean): Unit =
        analyzerValidationService
            .validateAnalyzersExistByIdAndAccountId(ids, accountId)
            .let { analyzerRepository.setAnalyzersActiveStatus(ids, status) }
            .let {
                ids.forEach { id ->
                    kafkaTemplate.send(
                        if (status) ACTIVATE_ANALYZER_TOPIC else DEACTIVATE_ANALYZER_TOPIC,
                        id
                    )
                }
            }

    fun deleteAnalyzers(accountId: String, ids: List<String>): Unit =
        analyzerValidationService
            .validateAnalyzersExistByIdAndAccountId(ids, accountId)
            .let { analyzerRepository.deleteByIdIn(ids) }
            .let { folderService.removeAnalyzers(ids.toSet()) }
            .let { ids.forEach { id -> kafkaTemplate.send(DEACTIVATE_ANALYZER_TOPIC, id) } }

    fun getAnalyzer(id: String, accountId: String): GridTableAnalyzerResponse =
        GridTableAnalyzerResponse(
            analyzerRepository.findByIdAndAccountId(id, accountId)
                ?: throw AnalyzerNotFoundException("Analyzer '$id' is not found")
        )

    fun createAnalyzer(accountId: String, analyzerData: CreateAnalyzerRequest) =
        analyzerData.apply {
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

    fun resetAnalyzers(accountId: String, ids: List<String>) {
        analyzerValidationService
            .validateAnalyzersExistByIdAndAccountId(ids, accountId)
            .let { analyzerRepository.setAnalyzersActiveStatus(ids, false) }
            .let {
                ids.forEach { id -> kafkaTemplate.send(DEACTIVATE_ANALYZER_TOPIC, id) }
            }
            .let { analyzerRepository.resetAnalyzers(ids) }
            .let {
                ids.forEach { id -> kafkaTemplate.send(ACTIVATE_ANALYZER_TOPIC, id) }
            }
    }

    fun bulkCreate(accountId: String, request: CreateAnalyzerBulkRequest) {
        with(request) {
            val symbols = symbolRepository.findAllById(symbols)
            val analyzersToInsert = mutableListOf<GridTableAnalyzerDocument>()

            for (symbol in symbols) {
                val currentPrice = runBlocking {
                    (if (demoAccount) publicBybitTestClient else publicBybitClient).getPairCurrentPrice(symbol.symbol)
                }
                for (stopLoss in stopLossMin..stopLossMax step stopLossStep) {
                    for (takeProfit in takeProfitMin..takeProfitMax step takeProfitStep) {
                        for (diapason in diapasonMin..diapasonMax step diapasonStep) {
                            for (gridSize in gridSizeMin..gridSizeMax step gridSizeStep) {
                                for (multiplier in multiplierMin..multiplierMax step multiplierStep) {
                                    val startCapital = startCapital.toDouble()
                                    val moneyPerOrder = startCapital.plusPercent(-2) / gridSize
                                    val qty = moneyPerOrder * multiplier / currentPrice

                                    if (qty > symbol.minOrderQty) {
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

            analyzerRepository.insert(analyzersToInsert)
            folders.forEach {
                folderService.addAnalyzersToFolder(
                    accountId,
                    it,
                    analyzersToInsert.map { analyzer -> analyzer.id }.toSet()
                )
            }

            if (request.active) {
                kafkaTemplate.send(ACTIVATE_ANALYZERS_TOPIC, analyzersToInsert.map { it.id }.joinToString { "," })
            }
        }
    }
}
