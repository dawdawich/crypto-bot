package space.dawdawich.strategy

import space.dawdawich.model.strategy.StrategyRuntimeInfoModel
import space.dawdawich.model.analyzer.PositionModel
import space.dawdawich.model.strategy.StrategyConfigModel
import space.dawdawich.strategy.model.Position
import space.dawdawich.strategy.model.Trend
import kotlin.math.absoluteValue
import kotlin.properties.Delegates


abstract class StrategyRunner(
    protected var money: Double,
    protected val multiplier: Int,
    protected val moneyChangeFunction: (Double, Double) -> Unit,
    val symbol: String,
    val simulateTradeOperations: Boolean,
    val id: String,
) {
    protected var currentPrice = 0.0
    var position: Position? = null

    abstract fun acceptPriceChange(previousPrise: Double, currentPrice: Double)

    fun updatePosition(position: Position) {
        if (!simulateTradeOperations) {
            this.position = position
        }
    }

    fun updateMoney(money: Double) {
        if (!simulateTradeOperations) {
            this.money = money
        }
    }

    fun getActivePositionTrend() = position?.trend

    open fun getRuntimeInfo() = StrategyRuntimeInfoModel(
        id,
        currentPrice,
        position?.convertToInfo())

    open fun getStrategyConfig() = StrategyConfigModel(id, symbol, money, multiplier)

    protected var moneyWithProfit: Double by Delegates.observable(money) { _, _, newValue ->
        if ((newValue - moneyHandler).absoluteValue > moneyHandler * 0.01) {
            moneyHandler = newValue
        }
    }

    protected fun Position.convertToInfo() = this.let { PositionModel(it.trend == Trend.LONG, it.size, it.entryPrice) }

    private var moneyHandler: Double by Delegates.observable(money) { _, old, new ->
        moneyChangeFunction(old, new)
    }
}
