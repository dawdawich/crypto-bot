package space.dawdawich.service

import kotlinx.coroutines.runBlocking
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.ACTIVATE_ANALYZER_TOPIC
import space.dawdawich.constants.DEACTIVATE_ANALYZER_TOPIC
import space.dawdawich.controller.model.AnalyzerBulkCreateRequest
import space.dawdawich.controller.model.CreateAnalyzerRequest
import space.dawdawich.controller.model.GridTableAnalyzerResponse
import space.dawdawich.exception.model.AnalyzerNotFoundException
import space.dawdawich.integration.client.bybit.ByBitPublicHttpClient
import space.dawdawich.repositories.mongo.GridTableAnalyzerRepository
import space.dawdawich.repositories.mongo.SymbolRepository
import space.dawdawich.repositories.mongo.entity.GridTableAnalyzerDocument
import space.dawdawich.service.validation.AnalyzerValidationService
import space.dawdawich.utils.plusPercent
import java.util.*

@Service
class AnalyzerService(
    private val gridTableAnalyzerRepository: GridTableAnalyzerRepository,
    private val analyzerValidationService: AnalyzerValidationService,
    private val symbolRepository: SymbolRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val publicBybitClient: ByBitPublicHttpClient,
    private val publicBybitTestClient: ByBitPublicHttpClient
) {

    fun getTopAnalyzers(): List<GridTableAnalyzerResponse> =
        gridTableAnalyzerRepository.findAllByPublic().sortedByDescending {
            val difference = it.money - it.startCapital
            val percentDifference = (difference / it.startCapital) * 100
            // The data is sorted by percent difference in descending order
            percentDifference
        }.map { GridTableAnalyzerResponse(it) }.take(20)

    fun getAnalyzers(accountId: String, page: Int, size: Int): List<GridTableAnalyzerResponse> =
        gridTableAnalyzerRepository.findAllByAccountId(accountId, PageRequest.of(page, size))
            .map { GridTableAnalyzerResponse(it) }.toList()

    fun getAnalyzersCount(accountId: String) = gridTableAnalyzerRepository.countByAccountId(accountId)

    fun updateAnalyzerStatus(accountId: String, id: String, status: Boolean): Unit =
            analyzerValidationService
                    .validateAnalyzerExistByIdAndAccountId(id, accountId)
                    .let { gridTableAnalyzerRepository.setAnalyzerActiveStatus(id, status) }
                    .let { kafkaTemplate.send(if (status) ACTIVATE_ANALYZER_TOPIC else DEACTIVATE_ANALYZER_TOPIC, id) }

    fun deleteAnalyzer(accountId: String, id: String): Unit =
            analyzerValidationService
                    .validateAnalyzerExistByIdAndAccountId(id, accountId)
                    .let { kafkaTemplate.send(DEACTIVATE_ANALYZER_TOPIC, id) }

    fun getAnalyzer(id: String, accountId: String): GridTableAnalyzerResponse =
        GridTableAnalyzerResponse(
            gridTableAnalyzerRepository.findByIdAndAccountId(id, accountId)
                ?: throw AnalyzerNotFoundException("Analyzer '$id' is not found")
        )

    fun createAnalyzer(accountId: String, analyzerData: CreateAnalyzerRequest) =
        analyzerData.apply {
            val gridTableAnalyzerDocument = GridTableAnalyzerDocument(
                UUID.randomUUID().toString(),
                accountId,
                public,
                diapason,
                gridSize,
                multiplayer,
                stopLoss,
                takeProfit,
                symbolRepository.findByIdOrNull(symbol)!!,
                startCapital,
                active,
                demoAccount,
                market
            )
            gridTableAnalyzerRepository.insert(gridTableAnalyzerDocument)
            if (active) {
                kafkaTemplate.send(ACTIVATE_ANALYZER_TOPIC, gridTableAnalyzerDocument.id)
            }
        }

    fun bulkCreate(accountId: String, request: AnalyzerBulkCreateRequest) {
        val symbols = symbolRepository.findAllById(request.symbols)
        val analyzersToInsert = mutableListOf<GridTableAnalyzerDocument>()

        for (symbol in symbols) {
            val currentPrice = runBlocking {
                (if (request.demoAccount) publicBybitTestClient else publicBybitClient).getPairCurrentPrice(symbol.symbol)
            }
            for (stopLoss in request.minStopLoss..request.maxStopLoss) {
                for (takeProfit in request.minTakeProfit..request.maxTakeProfit) {
                    for (diapason in request.startDiapasonPercent..request.endDiapasonPercent) {
                        for (gridSize in request.fromGridSize..request.toGridSize step request.gridSizeStep) {
                            for (multiplier in request.multiplierFrom..request.multiplierTo) {
                                val startCapital = request.startCapital.toDouble()
                                val moneyPerOrder = startCapital.plusPercent(-2) / gridSize
                                val qty = moneyPerOrder * multiplier / currentPrice

                                if (qty > symbol.minOrderQty) {
                                    analyzersToInsert.add(
                                        GridTableAnalyzerDocument(
                                            UUID.randomUUID().toString(),
                                            accountId,
                                            true,
                                            diapason,
                                            gridSize,
                                            multiplier,
                                            stopLoss,
                                            takeProfit,
                                            symbol,
                                            startCapital,
                                            false,
                                            request.demoAccount,
                                            request.market
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        gridTableAnalyzerRepository.insert(analyzersToInsert)
    }

    fun changeAllAnalyzersStatus(accountId: String, status: Boolean) =
        gridTableAnalyzerRepository.setAllAnalyzersActiveStatus(accountId, status)

    fun deleteAnalyzers(accountId: String) = gridTableAnalyzerRepository.deleteByAccountId(accountId)
}
