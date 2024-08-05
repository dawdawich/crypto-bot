package space.dawdawich.strategy.strategies

import mu.KLogger
import mu.KotlinLogging
import space.dawdawich.strategy.model.*
import space.dawdawich.utils.calculatePercentageDifference
import space.dawdawich.utils.plusPercent
import kotlin.math.abs

class RSIOutBoundStrategyRunner(
    var money: Double,
    private val multiplier: Double,
    private val simulateTradeOperations: Boolean,
    private val kLineDuration: Int,
    symbolsToWork: List<String>,
    private val upperBound: Double,
    private val upperSellBound: Double,
    private val lowerBound: Double,
    private val lowerSellBound: Double,
    private val stopLoss: Int,
    val id: String,
//    private val moneyChangeFunction: MoneyChangePostProcessFunction = { _, _ -> },
    private val createOrderFunction: CreateOrderFunction = { inPrice: Double, orderSymbol: String, qty: Double, refreshTokenUpperBorder: Double, refreshTokenLowerBorder: Double, trend: Trend ->
        Order(inPrice, orderSymbol, qty, refreshTokenUpperBorder, refreshTokenLowerBorder, trend)
    },
//    private val cancelOrderFunction: CancelOrderFunction = { _, _ -> true },
) {
    private val logger: KLogger = KotlinLogging.logger {}

    private val positions: MutableMap<String, Position?> =
        mutableMapOf(*symbolsToWork.map { it to null }.toTypedArray())
    private val rsiS: MutableMap<String, Double> = mutableMapOf(*symbolsToWork.map { it to Double.NaN }.toTypedArray())
    private val prices: MutableMap<String, Double> =
        mutableMapOf(*symbolsToWork.map { it to Double.NaN }.toTypedArray())
    private var closePositionFunction: (symbol: String) -> Unit = { symbol ->
        positions[symbol] = null
    }

    fun setClosePositionFunction(function: (symbol: String) -> Unit) {
        closePositionFunction = function
    }

    fun updatePosition(symbol: String, position: Position?) {
        positions[symbol] = position
    }

    fun updateMoney(money: Double) {
        this.money = money
    }

    fun acceptKLine(symbol: String, kLine: KLine) {
        val oldRsi = rsiS[symbol]!!
        val newRsi = kLine.rsi
        rsiS[symbol] = newRsi

        if (!oldRsi.isNaN() && !newRsi.isNaN() && oldRsi != newRsi && !prices[symbol]!!.isNaN()) {
            if (positions[symbol] == null) {
                val candlePercentageChange = abs(kLine.openPrice.calculatePercentageDifference(kLine.closePrice))
                val moneyPerOrder = money * ((5 + candlePercentageChange) / 100)

                val exceedUpperBound = upperBound in oldRsi..newRsi
                val exceedLowerBound = lowerBound in newRsi..oldRsi
                val positionsValue = positions.values.filterNotNull().sumOf { it.getPositionValue() }
                if (positionsValue / multiplier + moneyPerOrder < money && (exceedUpperBound || exceedLowerBound) && candlePercentageChange > 2) {
                    val trend = if (exceedUpperBound) Trend.SHORT else Trend.LONG
                    val qty = moneyPerOrder * multiplier / prices[symbol]!!

                    logger.debug { "Try to create order. Symbol: $symbol, oldRsi: $oldRsi, newRsi: $newRsi, trend: $trend" }
                    val order = createOrderFunction(prices[symbol]!!, symbol, qty, 0.0, 0.0, trend)

                    if (simulateTradeOperations) {
                        positions[symbol] = Position(order!!.inPrice, order.count, order.trend)
                    }
                }
            } else {
                val position = positions[symbol]
                if (position != null && (position.trend == Trend.LONG && lowerSellBound in oldRsi..newRsi ||
                            (position.trend == Trend.SHORT && upperSellBound in newRsi..oldRsi))
                ) {
                    closePositionFunction(symbol)
                    if (simulateTradeOperations) {
                        money += position.calculateProfit(prices[symbol]!!)
                    }
                }
            }
        }
    }

    fun acceptPriceChange(symbol: String, currentPrice: Double, rsi: Double) {
        prices[symbol] = currentPrice

        positions[symbol]?.let { position ->
            val profit = position.calculateProfit(currentPrice)
            val moneyWithProfit = money + profit
            if (moneyWithProfit <= money.plusPercent(-stopLoss) ||
                ((position.trend == Trend.LONG && rsi >= lowerSellBound) || (position.trend == Trend.SHORT && rsi <= upperSellBound))
            ) {
                closePositionFunction(symbol)
                if (simulateTradeOperations) {
                    money += profit
                }
            }
        }
    }

    fun getPosition(symbol: String) = positions[symbol]

    fun getCurrentPrice(symbol: String) = prices[symbol]
}
