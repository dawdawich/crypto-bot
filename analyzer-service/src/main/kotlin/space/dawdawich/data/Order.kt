package space.dawdawich.data

import java.util.*

data class Order(var inPrice: Double, val pair: String, val count: Double, var stopLoss: Double, var takeProfit: Double, val trend: Trend, var isFilled: Boolean = false, val createTime: Long = System.currentTimeMillis(), var id: String = UUID.randomUUID().toString()) {

    fun isStopLossExceeded(currentPrice: Double): Boolean {
        return (trend == Trend.LONG && currentPrice <= stopLoss) || (trend == Trend.SHORT && currentPrice >= stopLoss)
    }

    fun isTakeProfitExceeded(currentPrice: Double): Boolean {
        return (trend == Trend.LONG && currentPrice >= takeProfit) || (trend == Trend.SHORT && currentPrice <= takeProfit)
    }

    fun calculateProfit(currentPrice: Double): Double {
        return if (trend == Trend.LONG) {
            // -----------profit-----------------------------fee---------------
            (currentPrice - inPrice) * count - (count * currentPrice * 0.00055)
        } else {
            (inPrice - currentPrice) * count - (count * currentPrice * 0.00055)
        }
    }
}
