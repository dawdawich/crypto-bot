package space.dawdawich.strategy

import space.dawdawich.strategy.model.CancelOrderFunction
import space.dawdawich.strategy.model.CreateOrderFunction
import space.dawdawich.strategy.model.MoneyChangePostProcessFunction


abstract class PriceChangeStrategyRunner(
    protected val priceMinStep: Double,
    minQtyStep: Double,
    money: Double,
    multiplier: Int,
    moneyChangeFunction: MoneyChangePostProcessFunction,
    createOrderFunction: CreateOrderFunction,
    cancelOrderFunction: CancelOrderFunction,
    symbol: String,
    simulateTradeOperations: Boolean,
    id: String,
) : StrategyRunner(money, multiplier, moneyChangeFunction, createOrderFunction, cancelOrderFunction, minQtyStep, symbol, simulateTradeOperations, id) {
    protected var currentPrice = 0.0
}
