package space.dawdawich.service.validation

import org.springframework.stereotype.Service
import space.dawdawich.exception.model.AnalyzerNotFoundException
import space.dawdawich.repositories.GridTableAnalyzerRepository
import kotlin.jvm.Throws

@Service
class AnalyzerValidationService(
        private val analyzerRepository: GridTableAnalyzerRepository,
) {
    @Throws(AnalyzerNotFoundException::class)
    fun <T> validateAnalyzersExistByIdsAndAccountId(analyzerIds: Set<String>, accountId: String, action: () -> T): T {
        if (!analyzerIds.all { analyzerRepository.existsByIdAndAccountId(it, accountId) }) {
            throw AnalyzerNotFoundException("This list of analyzers are not found: $analyzerIds")
        }
        return action()
    }

    @Throws(AnalyzerNotFoundException::class)
    fun <T> validateAnalyzerExistByIdAndAccountId(analyzerId: String, accountId: String, action: () -> T): T {
        if (!analyzerRepository.existsByIdAndAccountId(analyzerId, accountId)) {
            throw AnalyzerNotFoundException("Analyzer with id '$analyzerId' is not found")
        }
        return action()
    }
}
