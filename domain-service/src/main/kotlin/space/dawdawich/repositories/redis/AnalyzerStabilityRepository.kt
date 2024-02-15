package space.dawdawich.repositories.redis

import org.springframework.data.repository.CrudRepository
import space.dawdawich.repositories.redis.entity.AnalyzerStabilityCoefModel

interface AnalyzerStabilityRepository: CrudRepository<AnalyzerStabilityCoefModel, String> {
}