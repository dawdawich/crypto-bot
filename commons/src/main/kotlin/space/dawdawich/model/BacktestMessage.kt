package space.dawdawich.model

import kotlinx.serialization.Serializable

@Serializable
data class BacktestMessage(
    val requestId: String,
    val symbols: List<String>,
    val startCapital: Double,
    val multiplier: Double,
    val diapason: Int,
    val gridSize: Int,
    val takeProfit: Int,
    val stopLoss: Int,
    val startTime: Long,
) : java.io.Serializable
