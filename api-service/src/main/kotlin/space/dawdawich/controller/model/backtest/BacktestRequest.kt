package space.dawdawich.controller.model.backtest

import kotlinx.serialization.Serializable

@Serializable
data class BacktestRequest(
    val symbol: String,
    val startCapital: Double,
    val multiplier: Int,
    val diapason: Int,
    val gridSize: Int,
    val takeProfit: Int,
    val stopLoss: Int,
    val startTime: Long,
)
