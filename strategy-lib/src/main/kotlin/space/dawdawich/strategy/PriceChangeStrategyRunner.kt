package space.dawdawich.strategy


abstract class PriceChangeStrategyRunner(
    money: Double,
    multiplier: Int,
    symbol: String,
    minQtyStep: Double,
    id: String,
) : StrategyRunner(money, multiplier, minQtyStep, symbol, id) {
    abstract fun acceptPriceChange(currentPrice: Double)
}
