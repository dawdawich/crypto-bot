package space.dawdawich.utils

import space.dawdawich.analyzers.KLineStrategyAnalyzer
import space.dawdawich.analyzers.PriceChangeStrategyAnalyzer
import space.dawdawich.repositories.mongo.entity.CandleTailStrategyAnalyzerDocument
import space.dawdawich.repositories.mongo.entity.GridTableAnalyzerDocument
import space.dawdawich.strategy.model.MoneyChangePostProcessFunction
import space.dawdawich.strategy.model.UpdateMiddlePricePostProcessFunction
import space.dawdawich.strategy.strategies.CandleTailStrategyRunner
import space.dawdawich.strategy.strategies.GridTableStrategyRunner

fun GridTableAnalyzerDocument.convert(
    changeMoneyCallback: MoneyChangePostProcessFunction,
    updateMiddlePrice: UpdateMiddlePricePostProcessFunction,
): PriceChangeStrategyAnalyzer = PriceChangeStrategyAnalyzer(
    GridTableStrategyRunner(
        symbolInfo.symbol,
        diapason,
        gridSize,
        positionStopLoss,
        positionTakeProfit,
        multiplier,
        money,
        symbolInfo.tickSize,
        symbolInfo.minOrderQty,
        true,
        moneyChangePostProcessFunction = changeMoneyCallback,
        updateMiddlePrice = updateMiddlePrice,
        id = id
    ),
    0.0,
    startCapital,
    symbolInfo.symbol,
    accountId,
    market,
    demoAccount,
    id,
)

fun CandleTailStrategyAnalyzerDocument.convert(
    changeMoneyCallback: MoneyChangePostProcessFunction,
): KLineStrategyAnalyzer = KLineStrategyAnalyzer(
    CandleTailStrategyRunner(
        money,
        multiplier,
        symbolInfo.symbol,
        true,
        kLineDuration,
        positionStopLoss,
        positionTakeProfit,
        symbolInfo.minOrderQty,
        id,
        changeMoneyCallback
    ),
    0.0,
    startCapital,
    symbolInfo.symbol,
    accountId,
    market,
    demoAccount,
    id
)
