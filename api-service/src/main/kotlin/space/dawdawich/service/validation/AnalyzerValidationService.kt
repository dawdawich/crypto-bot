package space.dawdawich.service.validation

import org.springframework.stereotype.Service
import space.dawdawich.exception.model.AnalyzerNotFoundException
import space.dawdawich.repositories.AnalyzerRepository
import kotlin.jvm.Throws

@Service
class AnalyzerValidationService(
    private val analyzerRepository: AnalyzerRepository,
) {
    @Throws(AnalyzerNotFoundException::class)
    fun validateAnalyzersExistByIdsAndAccountId(analyzerIds: Set<String>, accountId: String) {
        if (!analyzerIds.all { analyzerRepository.existsByIdAndAccountId(it, accountId) }) {
            throw AnalyzerNotFoundException("This list of analyzers are not found: $analyzerIds")
        }
    }

    @Throws(AnalyzerNotFoundException::class)
    fun validateAnalyzerExistByIdAndAccountId(analyzerId: String, accountId: String) {
        if (!analyzerRepository.existsByIdAndAccountId(analyzerId, accountId)) {
            throw AnalyzerNotFoundException("Analyzer with id '$analyzerId' is not found")
        }
    }

    @Throws(AnalyzerNotFoundException::class)
    fun validateAnalyzersExistByIdAndAccountId(analyzerIds: List<String>, accountId: String) {
        if (!analyzerRepository.existsByIdInAndAccountId(analyzerIds, accountId)) {
            throw AnalyzerNotFoundException("Analyzer with id '$analyzerIds' is not found")
        }
    }
}
