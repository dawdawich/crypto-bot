package space.dawdawich.controller.model.backtest

import kotlinx.serialization.Serializable

@Serializable
data class BacktestResultDetail(
    val diapason: Int,
    val gridSize: Int,
    val takeProfit: Int,
    val stopLoss: Int,
    val symbol: String,
    val leverage: Double,
    val resultCapital: Double,
    val startPriceTime: Long,
    val endPriceTime: Long,
)
