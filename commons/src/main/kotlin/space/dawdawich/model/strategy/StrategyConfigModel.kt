package space.dawdawich.model.strategy

import kotlinx.serialization.Serializable

/**
 * Represents the base class for strategy config models. Uses as base DTO object.
 */
@Serializable
sealed class StrategyConfigModel : java.io.Serializable {
    abstract val id: String
    abstract val symbol: String
    abstract val money: Double
    abstract val multiplier: Int
    abstract val stopLoss: Int
    abstract val takeProfit: Int
    abstract val minQtyStep: Double
}
