package space.dawdawich.service.validation

import org.springframework.stereotype.Service
import space.dawdawich.exception.model.AnalyzerNotFoundException
import space.dawdawich.repositories.GridTableAnalyzerRepository

@Service
class AnalyzerValidationService(
        private val analyzerRepository: GridTableAnalyzerRepository,
) {
    fun validateAnalyzersExistByIds(analyzerIds: Set<String>, accountId: String) {
        if (!analyzerIds.all { analyzerRepository.existsByIdAndAccountId(it, accountId) })
            throw AnalyzerNotFoundException("This list of analyzers are not found: $analyzerIds")
    }

    fun validateAnalyzerExistById(analyzerId: String, accountId: String) {
        if (!analyzerRepository.existsByIdAndAccountId(analyzerId, accountId))
            throw AnalyzerNotFoundException("Analyzer with id '$analyzerId' is not found")
    }
}
