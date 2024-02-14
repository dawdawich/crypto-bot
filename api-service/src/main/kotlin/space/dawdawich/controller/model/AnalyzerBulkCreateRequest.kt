package space.dawdawich.controller.model

import kotlinx.serialization.Serializable
import space.dawdawich.model.constants.Market

@Serializable
data class AnalyzerBulkCreateRequest(
    val symbols: List<String>,
    val minStopLoss: Int,
    val maxStopLoss: Int,
    val minTakeProfit: Int,
    val maxTakeProfit: Int,
    val startDiapasonPercent: Int,
    val endDiapasonPercent: Int,
    val fromGridSize: Int,
    val toGridSize: Int,
    val gridSizeStep: Int,
    val multiplierFrom: Int,
    val multiplierTo: Int,
    val startCapital: Int,
    val demoAccount: Boolean,
    val market: Market
)
