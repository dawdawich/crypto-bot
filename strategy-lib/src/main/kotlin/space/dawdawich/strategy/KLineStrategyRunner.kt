package space.dawdawich.strategy

import space.dawdawich.strategy.model.CancelOrderFunction
import space.dawdawich.strategy.model.CreateOrderFunction
import space.dawdawich.strategy.model.KLine
import space.dawdawich.strategy.model.MoneyChangePostProcessFunction

abstract class KLineStrategyRunner(
    money: Double,
    multiplier: Int,
    moneyChangeFunction: MoneyChangePostProcessFunction,
    createOrderFunction: CreateOrderFunction,
    cancelOrderFunction: CancelOrderFunction,
    minQtyStep: Double,
    symbol: String,
    simulateTradeOperations: Boolean,
    protected val kLineDuration: Int,
    id: String,
) : StrategyRunner(money, multiplier, moneyChangeFunction,createOrderFunction, cancelOrderFunction, minQtyStep, symbol, simulateTradeOperations, id) {
    abstract fun acceptKLine(kLine: KLine)
}
