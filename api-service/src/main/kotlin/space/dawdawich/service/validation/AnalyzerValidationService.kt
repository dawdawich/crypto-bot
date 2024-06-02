package space.dawdawich.service.validation

import org.springframework.stereotype.Service
import space.dawdawich.exception.model.AnalyzerNotFoundException
import space.dawdawich.repositories.mongo.AnalyzerRepository
import kotlin.jvm.Throws

/**
 * AnalyzerValidationService class provides methods to validate the existence of analyzers.
 *
 * @param analyzerRepository The repository for Analyzer document.
 */
@Service
class AnalyzerValidationService(private val analyzerRepository: AnalyzerRepository) {

    /**
     * Validates if all analyzers with the given IDs and account ID exist in the system.
     *
     * @param analyzerIds The set of analyzer IDs to validate.
     * @param accountId The account ID associated with the analyzers.
     * @throws AnalyzerNotFoundException if any of the analyzers are not found.
     */
    @Throws(AnalyzerNotFoundException::class)
    fun validateAnalyzersExistByIdsAndAccountId(analyzerIds: Set<String>, accountId: String) {
        if (!analyzerIds.all { analyzerRepository.existsByIdAndAccountId(it, accountId) }) {
            throw AnalyzerNotFoundException("This list of analyzers are not found: $analyzerIds")
        }
    }

    /**
     * Validates if an analyzer with the given ID and account ID exists.
     *
     * @param analyzerId The ID of the analyzer to validate.
     * @param accountId The account ID associated with the analyzer.
     * @throws AnalyzerNotFoundException if the analyzer is not found.
     */
    @Throws(AnalyzerNotFoundException::class)
    fun validateAnalyzerExistByIdAndAccountId(analyzerId: String, accountId: String) {
        if (!analyzerRepository.existsByIdAndAccountId(analyzerId, accountId)) {
            throw AnalyzerNotFoundException("Analyzer with id '$analyzerId' is not found")
        }
    }

    /**
     * Validates if analyzers with the given IDs and account ID exist in the system.
     *
     * @param analyzerIds The list of analyzer IDs to validate.
     * @param accountId The account ID associated with the analyzers.
     * @throws AnalyzerNotFoundException if any of the analyzers are not found.
     */
    @Throws(AnalyzerNotFoundException::class)
    fun validateAnalyzersExistByIdAndAccountId(analyzerIds: List<String>, accountId: String) {
        if (!analyzerRepository.existsByIdInAndAccountId(analyzerIds, accountId)) {
            throw AnalyzerNotFoundException("Analyzer with id '$analyzerIds' is not found")
        }
    }
}
