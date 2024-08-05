package space.dawdawich.strategy.strategies

import space.dawdawich.model.strategy.RSIGridStrategyConfigModel
import space.dawdawich.model.strategy.RSIGridTableStrategyRuntimeInfoModel
import space.dawdawich.model.strategy.StrategyRuntimeInfoModel
import space.dawdawich.strategy.KLineStrategyRunner
import space.dawdawich.strategy.model.*
import space.dawdawich.utils.plusPercent
import kotlin.math.abs
import kotlin.properties.Delegates

class RSIGridTableStrategyRunner(
    money: Double,
    multiplier: Int,
    minQtyStep: Double,
    symbol: String,
    simulateTradeOperations: Boolean,
    kLineDuration: Int,
    private val gridSize: Int,
    private val stopLoss: Int,
    private val takeProfit: Int,
    id: String,
    moneyChangeFunction: MoneyChangePostProcessFunction = { _, _ -> },
    createOrderFunction: CreateOrderFunction = { inPrice: Double, orderSymbol: String, qty: Double, refreshTokenUpperBorder: Double, refreshTokenLowerBorder: Double, trend: Trend ->
        Order(inPrice, orderSymbol, qty, refreshTokenUpperBorder, refreshTokenLowerBorder, trend)
    },
    cancelOrderFunction: CancelOrderFunction = { _, _ -> true },
) : KLineStrategyRunner(
    money,
    multiplier,
    moneyChangeFunction,
    createOrderFunction,
    cancelOrderFunction,
    minQtyStep,
    symbol,
    simulateTradeOperations,
    kLineDuration,
    id
) {

    private val step = 100.0 / gridSize
    private val synchronizeObject: Any = Any()
    private val orderRSIGrid: MutableMap<Double, Boolean> = mutableMapOf()
    private var currentPrice: Double = Double.NaN
    private var currentRSI by Delegates.observable(Double.NaN) { _, oldValue, newValue ->
        if (oldValue != newValue && !oldValue.isNaN() && !newValue.isNaN() && !currentPrice.isNaN()) {
            processOrders(oldValue, newValue)
        }
    }

    init {
        var minRSI = 50.0
        var maxRSI = 50.0
        val gridRSI = mutableListOf<Double>()

        repeat(gridSize / 2) {
            minRSI -= step
            maxRSI += step
            gridRSI += minRSI
            gridRSI += maxRSI
        }

        orderRSIGrid += gridRSI.map { it to false }
    }

    override fun acceptKLine(kLine: KLine) {
        currentRSI = kLine.rsi
    }

    override fun acceptPriceChange(previousPrise: Double, currentPrice: Double) {
        this.currentPrice = currentPrice
        synchronized(synchronizeObject) {
            checkPosition(currentPrice)
        }
    }

    override fun getStrategyConfig() = RSIGridStrategyConfigModel(
        id, symbol, money, multiplier, stopLoss, takeProfit, minQtyStep, gridSize, kLineDuration
    )

    override fun getRuntimeInfo(): StrategyRuntimeInfoModel  = RSIGridTableStrategyRuntimeInfoModel(
        id, orderRSIGrid.keys.toList(), currentPrice, step, position?.convertToInfo()
    )

    private fun processOrders(oldRSI: Double, newRSI: Double) {
        val moneyPerOrder = money / gridSize

        if ((position?.getPositionValue() ?: 0.0) / multiplier + moneyPerOrder < money) {
            val filteredGrid = orderRSIGrid.entries
                .asSequence()
                .filter { it.key in oldRSI..newRSI || it.key in newRSI..oldRSI }
                .filter { !it.value }

            val count = filteredGrid.count()
            val longTrend = filteredGrid.count { it.key >= 50.0 }
            val shortTrend = filteredGrid.count { it.key < 50.0 }
            if (count > 0 && (longTrend > 0 || shortTrend > 0)) {
                val resultCount = longTrend - shortTrend
                if (resultCount == 0) return
                val trend = if (resultCount < 0) Trend.LONG else Trend.SHORT
                val qty = moneyPerOrder * abs(resultCount) * multiplier / currentPrice

                if (position?.trend != trend && ((position?.calculateROI(currentPrice, multiplier) ?: 10.0) < 10)) {
                    return
                }

                val order = createOrderFunction(currentPrice, symbol, qty, 0.0, 0.0, trend)

                if (simulateTradeOperations) {
                    position?.updateSizeAndEntryPrice(order!!) ?: run { position = Position(order!!.inPrice, order.count, order.trend) }
                }
                filteredGrid.forEach { orderRSIGrid[it.key] = true }
            }
        }

        orderRSIGrid.entries
            .filter { it.value }
            .filter { it.key !in (it.key - step)..(it.key + step) }
            .forEach { orderRSIGrid[it.key] = false }
    }

    private fun checkPosition(currentPrice: Double) {
        if (money > 0) {
            position?.let {
                val profit = it.calculateProfit(currentPrice)
                moneyWithProfit = money + profit

                if (moneyWithProfit <= money.plusPercent(-stopLoss)) {
                    closePositionFunction(true)
                    money += profit
                } else if (moneyWithProfit >= money.plusPercent(takeProfit)) {
                    closePositionFunction(false)
                    money += profit
                }
            }
        } else {
            money = 0.0
        }
    }
}
