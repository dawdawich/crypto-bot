package space.dawdawich.controller.model.analyzer

import kotlinx.serialization.Serializable
import space.dawdawich.model.constants.Market

@Serializable
data class CreateCandleTailAnalyzerRequest(
    val public: Boolean,
    val multiplier: Int,
    val stopLoss: Int,
    val takeProfit: Int,
    val kLineDuration: Int,
    val symbol: String,
    val startCapital: Double,
    val active: Boolean,
    val market: Market,
    val demoAccount: Boolean,
    val folders: List<String>,
) : CreateAnalyzerRequest()
