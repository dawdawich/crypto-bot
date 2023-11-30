package space.dawdawich.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import space.dawdawich.model.GridTableAnalyzer
import space.dawdawich.model.Position
import space.dawdawich.repositories.AnalyzerPositionRepository
import space.dawdawich.repositories.GridTableAnalyzerRepository

@Service
class AnalyzerService(
    private val gridTableAnalyzerRepository: GridTableAnalyzerRepository,
    private val analyzerPositionRepository: AnalyzerPositionRepository
) {

    fun getTopAnalyzers(): List<GridTableAnalyzer> {
        return gridTableAnalyzerRepository.findAllByOrderByMoneyDesc().get().map { GridTableAnalyzer(it) }.toList()
    }

    // TODO: Add check for null and throw not found exception
    fun getAnalyzer(id: String): GridTableAnalyzer = GridTableAnalyzer(gridTableAnalyzerRepository.findByIdOrNull(id)!!)

    fun getCompletePositionsForAnalyzer(analyzerId: String): List<Position> =
        analyzerPositionRepository.getAllByAnalyzerId(analyzerId).map {
            Position(it.positionEntryPrice, it.positionSize, it.closePrice, it.isLong, it.createTime, it.closeTime)
        }
}
