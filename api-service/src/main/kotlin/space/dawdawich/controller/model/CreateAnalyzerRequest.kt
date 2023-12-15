package space.dawdawich.controller.model

import kotlinx.serialization.Serializable

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
    val active: Boolean
)
