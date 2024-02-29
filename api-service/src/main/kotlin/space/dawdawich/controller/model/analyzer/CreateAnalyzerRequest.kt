package space.dawdawich.controller.model.analyzer

import kotlinx.serialization.Serializable
import space.dawdawich.model.constants.Market
import space.dawdawich.model.constants.TradeStrategy

@Serializable
data class CreateAnalyzerRequest(
    val public: Boolean,
    val diapason: Int,
    val gridSize: Int,
    val multiplier: Int,
    val stopLoss: Int,
    val takeProfit: Int,
    val symbol: String,
    val startCapital: Double,
    val active: Boolean,
    val market: Market,
    val demoAccount: Boolean,
    val folders: List<String>,
    val strategy: TradeStrategy,
)
