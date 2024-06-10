package space.dawdawich.controller.model.analyzer

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import space.dawdawich.model.constants.Market
import space.dawdawich.model.constants.TradeStrategy
import space.dawdawich.repositories.mongo.entity.GridTableAnalyzerDocument

/**
 * Represents the response object for the GridTableAnalyzerDocument.
 *
 * @property id The identifier of the analyzer.
 * @property diapason The diapason value of the analyzer.
 * @property gridSize The gridSize value of the analyzer.
 * @property multiplier The multiplier value of the analyzer.
 * @property positionStopLoss The positionStopLoss value of the analyzer.
 * @property positionTakeProfit The positionTakeProfit value of the analyzer.
 * @property symbol The symbol value of the analyzer.
 * @property startCapital The startCapital value of the analyzer.
 * @property money The money value of the analyzer.
 * @property isActive The isActive value of the analyzer.
 * @property demoAccount The demoAccount value of the analyzer.
 * @property strategy The strategy value of the analyzer.
 * @property market The market value of the analyzer.
 * @property public The public value of the analyzer.
 * @property createTime The createTime value of the analyzer.
 * @property updateTime The updateTime value of the analyzer.
 * @property stabilityCoef The stabilityCoef value of the analyzer.
 * @property pnl1 The pnl1 value of the analyzer.
 * @property pnl12 The pnl12 value of the analyzer.
 * @property pnl24 The pnl24 value of the analyzer.
 *
 * @constructor Creates a GridTableAnalyzerResponse object with a [GridTableAnalyzerDocument] object.
 */
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
    var stabilityCoef: Double = 0.0,
    @EncodeDefault
    var pnl1: Int = 0,
    @EncodeDefault
    var pnl12: Int = 0,
    @EncodeDefault
    var pnl24: Int = 0,
    val symbolVolatile: Double?
) {
    constructor(documentAnalyzer: GridTableAnalyzerDocument, volatileCoef: Double?) : this(
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
        documentAnalyzer.stabilityCoef ?: 0.0,
        documentAnalyzer.pNl1 ?: 0,
        documentAnalyzer.pNl12 ?: 0,
        documentAnalyzer.pNl24 ?: 0,
        volatileCoef
    )
}
