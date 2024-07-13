package space.dawdawich.analyzers

import space.dawdawich.model.constants.Market
import space.dawdawich.strategy.PriceChangeStrategyRunner
import space.dawdawich.utils.findLargestRange
import space.dawdawich.utils.findLowestRange
import java.util.*
import kotlin.properties.Delegates

class PriceChangeStrategyAnalyzer(
    priceChangeStrategyRunner: PriceChangeStrategyRunner,
    currentPrice: Double,
    startCapital: Double,
    symbol: String,
    accountId: String,
    market: Market,
    demoAccount: Boolean,
    id: String = UUID.randomUUID().toString()
) : Analyzer(priceChangeStrategyRunner, currentPrice, startCapital, symbol, accountId, market, demoAccount, id)
