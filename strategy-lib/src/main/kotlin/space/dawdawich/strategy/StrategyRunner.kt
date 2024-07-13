package space.dawdawich.strategy

import space.dawdawich.model.analyzer.PositionModel
import space.dawdawich.model.strategy.StrategyConfigModel
import space.dawdawich.model.strategy.StrategyRuntimeInfoModel
import space.dawdawich.strategy.model.*
import kotlin.properties.Delegates

abstract class StrategyRunner(
    var money: Double,
    protected val multiplier: Int,
    protected val moneyChangeFunction: MoneyChangePostProcessFunction,
    protected val createOrderFunction: CreateOrderFunction,
    protected val cancelOrderFunction: CancelOrderFunction,
    protected val minQtyStep: Double,
    val symbol: String,
    val simulateTradeOperations: Boolean,
    val id: String,
) {
    protected var closePositionFunction: ClosePositionFunction = {
        position = null
    }

    var position: Position? = null

    var moneyWithProfit: Double  by Delegates.observable(money) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            moneyChangeFunction(oldValue, newValue)
        }
    }
        protected set

    fun setClosePosition(function: ClosePositionFunction) {
        closePositionFunction = function
    }

    fun updatePosition(position: Position?) {
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

    protected fun Position.convertToInfo() = this.let { PositionModel(it.trend == Trend.LONG, it.size, it.entryPrice) }

    abstract fun acceptPriceChange(previousPrise: Double, currentPrice: Double)

    abstract fun getStrategyConfig(): StrategyConfigModel

    abstract fun getRuntimeInfo(): StrategyRuntimeInfoModel
}
