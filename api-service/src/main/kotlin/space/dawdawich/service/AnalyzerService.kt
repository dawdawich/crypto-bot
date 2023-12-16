package space.dawdawich.service

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.data.repository.findByIdOrNull
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.ACTIVATE_ANALYZER_TOPIC
import space.dawdawich.constants.ADD_ANALYZER_TOPIC
import space.dawdawich.constants.DEACTIVATE_ANALYZER_TOPIC
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
            val difference = it.startCapital - it.money
            val percentDifference = (difference / it.startCapital) * 100
            // The data is sorted by percent difference in descending order
            percentDifference
        }.map { GridTableAnalyzerResponse(it) }.take(20)

    fun getAnalyzers(accountId: String): List<GridTableAnalyzerResponse> =
        gridTableAnalyzerRepository.findAllByAccountId(accountId).map { GridTableAnalyzerResponse(it) }.toList()

    fun updateAnalyzerStatus(accountId: String, id: String, status: Boolean) {
        if (gridTableAnalyzerRepository.countByIdAndAccountId(id, accountId) > 0) {
            gridTableAnalyzerRepository.setAnalyzerActiveStatus(id, status)
            kafkaTemplate.send(if (status) ACTIVATE_ANALYZER_TOPIC else DEACTIVATE_ANALYZER_TOPIC, id)
        } else {
            throw Exception("Account '$accountId' does not an owner of analyzer '$id'") // TODO extract to exact exception
        }
    }

    fun deleteAnalyzer(id: String) {
        gridTableAnalyzerRepository.deleteById(id)
        kafkaTemplate.send(DEACTIVATE_ANALYZER_TOPIC, id)
    }

    // TODO: Add check for null and throw not found exception
    fun getAnalyzer(id: String): GridTableAnalyzerResponse = GridTableAnalyzerResponse(gridTableAnalyzerRepository.findByIdOrNull(id)!!)

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
            gridTableAnalyzerRepository.insert(gridTableAnalyzerDocument)
            if (active) {
                kafkaTemplate.send(ADD_ANALYZER_TOPIC, Json.encodeToString(gridTableAnalyzerDocument))
            }
        }
    }
}
