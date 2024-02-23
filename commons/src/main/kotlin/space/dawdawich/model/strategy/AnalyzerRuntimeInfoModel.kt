package space.dawdawich.model.strategy

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzerRuntimeInfoModel(
    val money: Double,
    val stability: Double?,
    val positionDirection: String? = null,
    val positionEntryPrice: Double? = null,
    val positionSize: Double? = null,
)
