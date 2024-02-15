package space.dawdawich.repositories.redis.entity

import org.springframework.data.annotation.Id

class AnalyzerStabilityCoefModel (
    @Id
    val id: String,
    val stabilityCoefficients: List<Double>
)