package space.dawdawich.strategy

import space.dawdawich.model.analyzer.PositionModel
import space.dawdawich.model.strategy.configModel.StrategyConfigModel
import space.dawdawich.model.strategy.runtimeModel.StrategyRuntimeInfoModel
import space.dawdawich.strategy.model.ClosePositionFunction
import space.dawdawich.strategy.model.MoneyChangePostProcessFunction
import space.dawdawich.strategy.model.Position
import kotlin.math.absoluteValue
import kotlin.properties.Delegates


abstract class StrategyRunner(
    money: Double,
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

    var money: Double  by Delegates.observable(money) { _, _, newValue ->
        if ((newValue - moneyHandler).absoluteValue > moneyHandler * 0.01) {
            moneyHandler = newValue
        }
    }
        protected set

    protected var closePositionFunction: ClosePositionFunction = {
        position = null
    }

    abstract fun acceptPriceChange(previousPrice: Double, currentPrice: Double)

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

    protected var moneyWithProfit: Double = money

    protected fun Position.convertToInfo() = this.let { PositionModel(it.trend.directionBoolean, it.size, it.entryPrice) }

    private var moneyHandler: Double by Delegates.observable(money) { _, old, new ->
        moneyChangeFunction(old, new)
    }
}
