package space.dawdawich.model

import kotlinx.serialization.Serializable

@Serializable
data class BacktestBulkMessage(
    val requestId: String,
    val symbol: List<String>,
    val startCapital: Double,
    val multiplier: Pair<Int, Int>,
    val diapason: Pair<Int, Int>,
    val gridSize: Pair<Int, Int>,
    val takeProfit: Pair<Int, Int>,
    val stopLoss: Pair<Int, Int>,
    val startTime: Long,
)
