package space.dawdawich.service

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.data.repository.findByIdOrNull
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.ACTIVATE_ANALYZER_TOPIC
import space.dawdawich.constants.ADD_ANALYZER_TOPIC
import space.dawdawich.constants.DEACTIVATE_ANALYZER_TOPIC
import space.dawdawich.controller.model.AnalyzerBulkCreateRequest
import space.dawdawich.controller.model.CreateAnalyzerRequest
import space.dawdawich.controller.model.GridTableAnalyzerResponse
import space.dawdawich.repositories.GridTableAnalyzerRepository
import space.dawdawich.repositories.SymbolRepository
import space.dawdawich.repositories.entity.GridTableAnalyzerDocument
import java.util.*

@Service
class AnalyzerService(
    private val gridTableAnalyzerRepository: GridTableAnalyzerRepository,
    private val symbolRepository: SymbolRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>
) {

    fun getTopAnalyzers(): List<GridTableAnalyzerResponse> =
        gridTableAnalyzerRepository.findAllByPublic().sortedByDescending {
            val difference = it.money - it.startCapital
            val percentDifference = (difference / it.startCapital) * 100
            // The data is sorted by percent difference in descending order
            percentDifference
        }.map { GridTableAnalyzerResponse(it) }.take(20)

    fun getAnalyzers(accountId: String): List<GridTableAnalyzerResponse> =
        gridTableAnalyzerRepository.findAllByAccountId(accountId).map { GridTableAnalyzerResponse(it) }.toList()

    fun updateAnalyzerStatus(accountId: String, id: String, status: Boolean) {
        if (gridTableAnalyzerRepository.countByIdAndAccountId(id, accountId) > 0) {
            gridTableAnalyzerRepository.setAnalyzerActiveStatus(
                id,
                status
            ) // TODO: move to analyzer service, change status in db after operation completion
            kafkaTemplate.send(if (status) ACTIVATE_ANALYZER_TOPIC else DEACTIVATE_ANALYZER_TOPIC, id)
        } else {
            throw Exception("Account '$accountId' does not an owner of analyzer '$id'") // TODO extract to exact exception
        }
    }

    fun deleteAnalyzer(id: String) {
        kafkaTemplate.send(DEACTIVATE_ANALYZER_TOPIC, id)
    }

    // TODO: Add check for null and throw not found exception
    fun getAnalyzer(id: String): GridTableAnalyzerResponse =
        GridTableAnalyzerResponse(gridTableAnalyzerRepository.findByIdOrNull(id)!!)

    fun createAnalyzer(accountId: String, analyzerData: CreateAnalyzerRequest) {
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
                active
            )
            kafkaTemplate.send(ADD_ANALYZER_TOPIC, Json.encodeToString(gridTableAnalyzerDocument))
        }
    }

    fun bulkCreate(accountId: String, request: AnalyzerBulkCreateRequest) {
        val symbols = symbolRepository.findAllById(request.symbols)
        val analyzersToInsert = mutableListOf<GridTableAnalyzerDocument>()

        for (symbol in symbols) {
            for (stopLoss in request.minStopLoss..request.maxStopLoss) {
                for (takeProfit in request.minTakeProfit..request.maxTakeProfit) {
                    for (diapason in request.startDiapasonPercent..request.endDiapasonPercent) {
                        for (gridSize in request.fromGridSize..request.toGridSize step request.gridSizeStep) {
                            for (multiplier in request.multiplierFrom..request.multiplierTo) {
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
                                        request.startCapital.toDouble(),
                                        false
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        gridTableAnalyzerRepository.insert(analyzersToInsert)
    }

    fun changeAllAnalyzersStatus(accountId: String, status: Boolean) = gridTableAnalyzerRepository.setAllAnalyzersActiveStatus(accountId, status)

    fun deleteAnalyzers(accountId: String) = gridTableAnalyzerRepository.deleteByAccountId(accountId)
}
