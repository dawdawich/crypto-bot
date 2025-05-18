package space.dawdawich.controller.model.backtest

import kotlinx.serialization.Serializable

@Serializable
data class BacktestResultDetail(
    val symbol: String,
    val leverage: Double,
    val resultCapital: Double,
    val startPriceTime: Long,
    val endPriceTime: Long,
)
