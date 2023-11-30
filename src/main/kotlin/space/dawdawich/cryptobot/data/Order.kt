package space.dawdawich.cryptobot.data

import space.dawdawich.cryptobot.util.generateId
import java.util.*

data class Order(var inPrice: Double, val pair: String, val count: Double, var stopLoss: Double, var takeProfit: Double, val trend: Trend, var isFilled: Boolean = false, val createTime: Long = System.currentTimeMillis(), var id: String = generateId()) {

    fun isStopLossExceeded(currentPrice: Double): Boolean {
        return (trend == Trend.BULL && currentPrice <= stopLoss) || (trend == Trend.BEAR && currentPrice >= stopLoss)
    }

    fun isTakeProfitExceeded(currentPrice: Double): Boolean {
        return (trend == Trend.BULL && currentPrice >= takeProfit) || (trend == Trend.BEAR && currentPrice <= takeProfit)
    }

    fun calculateProfit(currentPrice: Double): Double {
        return if (trend == Trend.BULL) {
            // -----------profit-----------------------------fee---------------
            (currentPrice - inPrice) * count - (count * currentPrice * 0.00055)
        } else {
            (inPrice - currentPrice) * count - (count * currentPrice * 0.00055)
        }
    }
}
