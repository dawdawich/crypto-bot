package space.dawdawich.controller.model.backtest

import kotlinx.serialization.Serializable

@Serializable
data class BacktestRequestResultsResponse(
    val startCapital: Double,
    val diapason: Int,
    val gridSize: Int,
    val takeProfit: Int,
    val stopLoss: Int,
    val results: List<BacktestResultDetail>
)
