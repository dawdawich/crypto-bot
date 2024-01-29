package space.dawdawich.model.strategy

import kotlinx.serialization.Serializable
import space.dawdawich.model.analyzer.PositionModel

@Serializable
open class StrategyRuntimeInfoModel(
    val id: String,
    val currentPrice: Double,
    val position: PositionModel?
) {
}
