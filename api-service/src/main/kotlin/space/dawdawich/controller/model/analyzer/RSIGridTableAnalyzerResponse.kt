package space.dawdawich.controller.model.analyzer

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import space.dawdawich.model.constants.Market
import space.dawdawich.model.constants.TradeStrategy
import space.dawdawich.repositories.mongo.entity.RSIGridTableAnalyzerDocument

@Serializable
class RSIGridTableAnalyzerResponse(
    override val id: String,
    @SerialName("kLineDuration") val kLineDuration: Int,
    override val multiplier: Int,
    val gridSize: Int,
    override val positionStopLoss: Int,
    override val positionTakeProfit: Int,
    override val symbol: String,
    override var startCapital: Double,
    override var money: Double,
    override var isActive: Boolean,
    override var demoAccount: Boolean,
    override val strategy: TradeStrategy,
    override val market: Market,
    override val public: Boolean,
    override val createTime: Long,
    override val updateTime: Long,
    @EncodeDefault
    override var stabilityCoef: Double = 0.0,
    @EncodeDefault
    override var pnl1: Int = 0,
    @EncodeDefault
    override var pnl12: Int = 0,
    @EncodeDefault
    override var pnl24: Int = 0,
    override val symbolVolatile: Double?
) : AnalyzerResponse() {
    constructor(documentAnalyzer: RSIGridTableAnalyzerDocument, volatileCoef: Double?) : this(
        documentAnalyzer.id,
        documentAnalyzer.kLineDuration,
        documentAnalyzer.multiplier,
        documentAnalyzer.gridSize,
        documentAnalyzer.positionStopLoss,
        documentAnalyzer.positionTakeProfit,
        documentAnalyzer.symbolInfo.symbol,
        documentAnalyzer.startCapital,
        documentAnalyzer.money,
        documentAnalyzer.isActive,
        documentAnalyzer.demoAccount,
        TradeStrategy.RSI_GRID_TABLE_STRATEGY,
        documentAnalyzer.market,
        documentAnalyzer.public,
        documentAnalyzer.createTime,
        documentAnalyzer.updateTime,
        documentAnalyzer.stabilityCoef ?: 0.0,
        documentAnalyzer.pNl1 ?: 0,
        documentAnalyzer.pNl12 ?: 0,
        documentAnalyzer.pNl24 ?: 0,
        volatileCoef
    )
}
