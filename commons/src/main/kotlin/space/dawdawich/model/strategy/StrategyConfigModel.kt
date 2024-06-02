package space.dawdawich.model.strategy

import kotlinx.serialization.Serializable

/**
 * Represents the base class for strategy config models. Uses as base DTO object.
 */
@Serializable
sealed class StrategyConfigModel {
    abstract val id: String
    abstract val symbol: String
    abstract val money: Double
    abstract val multiplier: Int
    abstract val priceMinStep: Double
    abstract val minQtyStep: Double
}
