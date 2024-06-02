package space.dawdawich.model.strategy

import kotlinx.serialization.Serializable
import space.dawdawich.model.analyzer.PositionModel

/**
 * Represents the base class for strategy runtime info. Uses as base DTO object.
 */
@Serializable
sealed class StrategyRuntimeInfoModel {
    abstract val id: String
    abstract val currentPrice: Double
    abstract val position: PositionModel?
}
