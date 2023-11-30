package space.dawdawich.model

import space.dawdawich.repositories.entity.GridTableAnalyzerDocument

data class GridTableAnalyzer(
    val id: String,
    val diapason: Int,
    val gridSize: Int,
    val multiplayer: Int,
    val positionStopLoss: Int,
    val positionTakeProfit: Int,
    val symbol: String,
    var money: Double,
    var isActive: Boolean
) {
    constructor(documentAnalyzer: GridTableAnalyzerDocument) : this(
        documentAnalyzer.id,
        documentAnalyzer.diapason,
        documentAnalyzer.gridSize,
        documentAnalyzer.multiplayer,
        documentAnalyzer.positionStopLoss,
        documentAnalyzer.positionTakeProfit,
        documentAnalyzer.symbolInfo.symbol,
        documentAnalyzer.money,
        documentAnalyzer.isActive,
    )
}
