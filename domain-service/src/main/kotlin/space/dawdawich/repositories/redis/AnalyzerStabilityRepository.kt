package space.dawdawich.repositories.redis

import org.springframework.data.repository.CrudRepository
import space.dawdawich.repositories.redis.entity.AnalyzerMoneyModel

interface AnalyzerStabilityRepository : CrudRepository<AnalyzerMoneyModel, String> {
    fun findAllByAnalyzerId(analyzerId: String): List<AnalyzerMoneyModel>
    fun findFirstByAnalyzerIdOrderByTimestampDesc(analyzerId: String): AnalyzerMoneyModel?
}
