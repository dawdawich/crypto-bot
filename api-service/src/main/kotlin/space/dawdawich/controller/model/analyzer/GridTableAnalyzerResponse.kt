package space.dawdawich.controller.model.analyzer

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import space.dawdawich.model.constants.Market
import space.dawdawich.model.constants.TradeStrategy
import space.dawdawich.repositories.mongo.entity.GridTableAnalyzerDocument

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class GridTableAnalyzerResponse(
    val id: String,
    val diapason: Int,
    val gridSize: Int,
    val multiplier: Int,
    val positionStopLoss: Int,
    val positionTakeProfit: Int,
    val symbol: String,
    var startCapital: Double,
    var money: Double,
    var isActive: Boolean,
    var demoAccount: Boolean,
    val strategy: TradeStrategy,
    val market: Market,
    val public: Boolean,
    val createTime: Long,
    val updateTime: Long,
    @EncodeDefault
    var stabilityCoef: Double? = 0.0,
    @EncodeDefault
    var pnl1: Int? = 0,
    @EncodeDefault
    var pnl12: Int? = 0,
    @EncodeDefault
    var pnl24: Int? = 0,
) {
    constructor(documentAnalyzer: GridTableAnalyzerDocument) : this(
        documentAnalyzer.id,
        documentAnalyzer.diapason,
        documentAnalyzer.gridSize,
        documentAnalyzer.multiplier,
        documentAnalyzer.positionStopLoss,
        documentAnalyzer.positionTakeProfit,
        documentAnalyzer.symbolInfo.symbol,
        documentAnalyzer.startCapital,
        documentAnalyzer.money,
        documentAnalyzer.isActive,
        documentAnalyzer.demoAccount,
        documentAnalyzer.strategy,
        documentAnalyzer.market,
        documentAnalyzer.public,
        documentAnalyzer.createTime,
        documentAnalyzer.updateTime,
        documentAnalyzer.stabilityCoef,
        documentAnalyzer.pNl1,
        documentAnalyzer.pNl12,
        documentAnalyzer.pNl24,
    )
}
