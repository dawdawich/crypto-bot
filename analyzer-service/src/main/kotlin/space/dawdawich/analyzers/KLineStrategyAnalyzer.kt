package space.dawdawich.analyzers

import space.dawdawich.model.analyzer.KLineRecord
import space.dawdawich.model.constants.Market
import space.dawdawich.strategy.KLineStrategyRunner
import space.dawdawich.strategy.model.KLine
import java.util.*

class KLineStrategyAnalyzer(
    strategyRunner: KLineStrategyRunner, currentPrice: Double, startCapital: Double, symbol: String, accountId: String,
    market: Market, demoAccount: Boolean, id: String = UUID.randomUUID().toString(),
) : Analyzer(strategyRunner, currentPrice, startCapital, symbol, accountId, market, demoAccount, id) {

    fun acceptCandle(candle: KLineRecord) =
        (strategyRunner as KLineStrategyRunner).acceptKLine(KLine(candle.open, candle.close, candle.high, candle.low, candle.rsi))
}
