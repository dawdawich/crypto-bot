package space.dawdawich.controller.model

import kotlinx.serialization.Serializable
import space.dawdawich.repositories.mongo.entity.GridTableAnalyzerDocument

@Serializable
data class GridTableAnalyzerResponse(
    val id: String,
    val diapason: Int,
    val gridSize: Int,
    val multiplayer: Int,
    val positionStopLoss: Int,
    val positionTakeProfit: Int,
    val symbol: String,
    var startCapital: Double,
    var money: Double,
    var isActive: Boolean,
    var stabilityCoef: Double? = 0.0
) {
    constructor(documentAnalyzer: GridTableAnalyzerDocument) : this(
        documentAnalyzer.id,
        documentAnalyzer.diapason,
        documentAnalyzer.gridSize,
        documentAnalyzer.multiplayer,
        documentAnalyzer.positionStopLoss,
        documentAnalyzer.positionTakeProfit,
        documentAnalyzer.symbolInfo.symbol,
        documentAnalyzer.startCapital,
        documentAnalyzer.money,
        documentAnalyzer.isActive,
        documentAnalyzer.stabilityCoef,
    )
}
