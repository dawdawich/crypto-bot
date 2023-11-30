package space.dawdawich.cryptobot.analyzer

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import space.dawdawich.cryptobot.data.KLineIntervals
import space.dawdawich.cryptobot.data.Order
import space.dawdawich.cryptobot.data.Trend
import space.dawdawich.cryptobot.manager.OrderManager
import space.dawdawich.cryptobot.util.plusPercent
import kotlin.math.abs

class VoltyExpanCloseStrategyAnalyzer(
    wallet: Double = 100.0,
    stopLossPercent: Float,
    takeProfitPercent: Float,
    ticksToSwitch: Int = 1,
    trend: Trend = Trend.BULL,
    currentPrice: Double = 0.0,
    multiplier: Int = 1,
    pair: String,
    val candleInterval: KLineIntervals,
    private val statisticLength: Int = 5,
    private val yFactor: Double = 1.0,
) : AnalyzerCore(wallet, stopLossPercent, takeProfitPercent, ticksToSwitch, trend, currentPrice, multiplier, pair) {

    private val trueRangeList: ArrayDeque<Double> = ArrayDeque(statisticLength)
    private var lowerBand: Double? = null
    private var upperBand: Double? = null
    private var previousClose: Double? = null

    override fun terminateOrder() {

    }

    override fun accept(t: Double) {
        currentPrice = t

        processPriceChange()
    }

    private fun processPriceChange() {
        order?.let {
            if (it.isTakeProfitExceeded(currentPrice) || it.isStopLossExceeded(currentPrice)) {
                wallet += it.calculateProfit(currentPrice)
                this.order = null
            }
        } ?: run {
            if (trueRangeList.size == statisticLength && upperBand != null && lowerBand != null) {
                if (currentPrice > upperBand!!) {
                    order = Order(currentPrice, pair, wallet * multiplier / currentPrice, currentPrice.plusPercent(-stopLossPercent / multiplier), currentPrice.plusPercent(takeProfitPercent / multiplier), Trend.BULL)
                    manager?.let { GlobalScope.launch { it.createOrder() } }
                } else if (currentPrice < lowerBand!!) {
                    order = Order(currentPrice, pair, wallet * multiplier / currentPrice, currentPrice.plusPercent(stopLossPercent / multiplier), currentPrice.plusPercent(-takeProfitPercent / multiplier), Trend.BEAR)
                    manager?.let { GlobalScope.launch { it.createOrder() } }
                }
            }
        }
    }

    fun addStatistic(currentHigh: Double, currentLow: Double, currentClose: Double) {
        if (previousClose == null) {
            previousClose = currentClose
            return
        }
        val trueRange = maxOf(currentHigh - currentLow, maxOf(abs(currentHigh - previousClose!!), abs(currentLow - previousClose!!)))

        trueRangeList.addFirst(trueRange)
        if (trueRangeList.size > statisticLength) {
            trueRangeList.removeLast()
        }

        if (trueRangeList.size == statisticLength) {
            val avgTrueRange = getAvgTrueRange()
            upperBand = currentClose + yFactor * avgTrueRange
            lowerBand = currentClose - yFactor * avgTrueRange
        }
        previousClose = currentClose
    }

    private fun getAvgTrueRange(): Double {
        return trueRangeList.sum() / trueRangeList.size
    }

    fun getInfoForUI(): Map<String, String> {
        return mapOf(
            "wallet" to wallet.toString(),
            "stopLoss" to stopLossPercent.toString(),
            "takeProfit" to takeProfitPercent.toString(),
            "multiplier" to multiplier.toString(),
            "interval" to candleInterval.toString(),
            "statLength" to statisticLength.toString(),
            "Y" to yFactor.toString(),
            "pair" to pair
        )
    }
}
