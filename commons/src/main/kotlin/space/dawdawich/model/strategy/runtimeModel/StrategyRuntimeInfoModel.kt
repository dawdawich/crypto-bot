package space.dawdawich.model.strategy.runtimeModel

import kotlinx.serialization.Serializable
import space.dawdawich.model.analyzer.PositionModel

@Serializable
sealed class StrategyRuntimeInfoModel {
    abstract val id: String
    abstract val currentPrice: Double
    abstract val position: PositionModel?
}
