package space.dawdawich.strategy

import space.dawdawich.model.strategy.StrategyRuntimeInfoModel
import space.dawdawich.model.analyzer.PositionModel
import space.dawdawich.model.strategy.StrategyConfigModel
import space.dawdawich.strategy.model.ClosePositionFunction
import space.dawdawich.strategy.model.MoneyChangePostProcessFunction
import space.dawdawich.strategy.model.Position
import space.dawdawich.strategy.model.Trend
import kotlin.math.absoluteValue
import kotlin.properties.Delegates


abstract class StrategyRunner(
    var money: Double,
    protected val multiplier: Int,
    protected val moneyChangeFunction: MoneyChangePostProcessFunction,
    protected val priceMinStep: Double,
    protected val minQtyStep: Double,
    val symbol: String,
    val simulateTradeOperations: Boolean,
    val id: String,
) {
    protected var currentPrice = 0.0
    var position: Position? = null

    var moneyWithProfit: Double  by Delegates.observable(money) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            moneyChangeFunction(oldValue, newValue)
        }
    }
        protected set

    protected var closePositionFunction: ClosePositionFunction = {
        position = null
    }

    abstract fun acceptPriceChange(previousPrise: Double, currentPrice: Double)

    abstract fun getRuntimeInfo(): StrategyRuntimeInfoModel

    abstract fun getStrategyConfig(): StrategyConfigModel

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
}
