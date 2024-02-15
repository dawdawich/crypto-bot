package space.dawdawich.controller.model

import kotlinx.serialization.Serializable
import space.dawdawich.model.constants.Market

@Serializable
data class CreateAnalyzerRequest(
    val public: Boolean,
    val diapason: Int,
    val gridSize: Int,
    val multiplayer: Int,
    val stopLoss: Int,
    val takeProfit: Int,
    val symbol: String,
    val startCapital: Double,
    val active: Boolean,
    val market: Market,
    val demoAccount: Boolean
)
