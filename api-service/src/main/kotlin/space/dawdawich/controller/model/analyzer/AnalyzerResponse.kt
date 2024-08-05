package space.dawdawich.controller.model.analyzer

import space.dawdawich.model.constants.Market
import space.dawdawich.model.constants.TradeStrategy
import space.dawdawich.repositories.mongo.entity.AnalyzerDocument
import space.dawdawich.repositories.mongo.entity.CandleTailStrategyAnalyzerDocument
import space.dawdawich.repositories.mongo.entity.GridTableAnalyzerDocument
import space.dawdawich.repositories.mongo.entity.RSIGridTableAnalyzerDocument

sealed class AnalyzerResponse {
    companion object {
        fun fromDocument(document: AnalyzerDocument, volatileCoefficient: Double?) = when (document) {
            is GridTableAnalyzerDocument -> GridTableAnalyzerResponse(document, volatileCoefficient)
            is CandleTailStrategyAnalyzerDocument -> CandleTailAnalyzerResponse(document, volatileCoefficient)
            is RSIGridTableAnalyzerDocument -> RSIGridTableAnalyzerResponse(document, volatileCoefficient)
        }
    }

    abstract val id: String
    abstract val multiplier: Int
    abstract val positionStopLoss: Int
    abstract val positionTakeProfit: Int
    abstract val symbol: String
    abstract var startCapital: Double
    abstract var money: Double
    abstract var isActive: Boolean
    abstract var demoAccount: Boolean
    abstract val strategy: TradeStrategy
    abstract val market: Market
    abstract val public: Boolean
    abstract val createTime: Long
    abstract val updateTime: Long
    abstract var stabilityCoef: Double
    abstract var pnl1: Int
    abstract var pnl12: Int
    abstract var pnl24: Int
    abstract val symbolVolatile: Double?
}
