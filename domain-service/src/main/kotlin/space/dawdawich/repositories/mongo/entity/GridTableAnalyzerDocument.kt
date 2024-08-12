package space.dawdawich.repositories.mongo.entity

import org.springframework.data.mongodb.core.mapping.Document
import space.dawdawich.model.constants.Market

@Document("analyzer")
class GridTableAnalyzerDocument(
    id: String,
    accountId: String,
    public: Boolean,
    val diapason: Int,
    val gridSize: Int,
    multiplier: Int,
    positionStopLoss: Int,
    positionTakeProfit: Int,
    symbolInfo: SymbolInfoDocument,
    startCapital: Double,
    isActive: Boolean,
    demoAccount: Boolean,
    market: Market,
    val middlePrice: Double? = null,
    money: Double = startCapital,
    stabilityCoef: Double? = null,
    pNl1: Int? = null,
    pNl12: Int? = null,
    pNl24: Int? = null,
    pNl10M: Int? = null,
    createTime: Long = System.currentTimeMillis(),
    updateTime: Long = createTime
) : AnalyzerDocument(id, accountId, public, multiplier, positionStopLoss, positionTakeProfit, symbolInfo, startCapital, isActive, demoAccount, market, money, stabilityCoef, pNl1, pNl12, pNl24, pNl10M, createTime, updateTime)
