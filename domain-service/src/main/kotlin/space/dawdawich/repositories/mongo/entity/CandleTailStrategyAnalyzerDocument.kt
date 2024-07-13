package space.dawdawich.repositories.mongo.entity

import org.springframework.data.mongodb.core.mapping.Document
import space.dawdawich.model.constants.Market

@Document("analyzer")
class CandleTailStrategyAnalyzerDocument(
    id: String,
    accountId: String,
    public: Boolean,
    multiplier: Int,
    positionStopLoss: Int,
    positionTakeProfit: Int,
    symbolInfo: SymbolInfoDocument,
    startCapital: Double,
    isActive: Boolean,
    demoAccount: Boolean,
    market: Market,
    val kLineDuration: Int,
    money: Double = startCapital,
    stabilityCoef: Double? = null,
    pNl1: Int? = null,
    pNl12: Int? = null,
    pNl24: Int? = null,
    createTime: Long = System.currentTimeMillis(),
    updateTime: Long = createTime
) : AnalyzerDocument(id, accountId, public, multiplier, positionStopLoss, positionTakeProfit, symbolInfo, startCapital, isActive, demoAccount, market, money, stabilityCoef, pNl1, pNl12, pNl24, createTime, updateTime)
