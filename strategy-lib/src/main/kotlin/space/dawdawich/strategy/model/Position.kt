package space.dawdawich.strategy.model

import kotlin.math.absoluteValue

class Position(
    var entryPrice: Double,
    var size: Double,
    val trend: Trend,
    realizedPnL: Double? = null,
) {
    private var realizedPnL = -(entryPrice * 0.00055 * size)

    init {
        realizedPnL?.let {
            this.realizedPnL = it
        }
    }


    fun updateSizeAndEntryPrice(order: Order) {
        if (order.trend == trend) {
            entryPrice = ((entryPrice * size) + (order.inPrice * order.count)) / (size + order.count)
            size += order.count
            realizedPnL -= order.inPrice * 0.00055 * order.count
        } else {
            val toReduce = if (order.count > size) size else order.count
            size -= toReduce
            val profit = if (order.trend.directionBoolean) entryPrice - order.inPrice else order.inPrice - entryPrice
            realizedPnL += (profit - (entryPrice - order.inPrice).absoluteValue * 0.00055) * toReduce
        }
    }

    fun calculateProfit(currentPrice: Double): Double {
        val profitPerUnit = if (trend.directionBoolean) currentPrice - entryPrice else entryPrice - currentPrice
        return (profitPerUnit - (currentPrice - entryPrice).absoluteValue * 0.00055) * size + realizedPnL
    }

    fun calculateROI(currentPrice: Double): Double {
        val finalValue = size * currentPrice
        val initialValue = entryPrice * size
        return ((finalValue - initialValue) / initialValue) * 100
    }

    fun calculateReduceOrder(orderPrice: Double, orderSize: Double, orderTrend: Trend): Double {
        return if (orderTrend != trend) {
            val toReduce = if (orderSize > size) size else orderSize
            val profit = if (orderTrend.directionBoolean) entryPrice - orderPrice else orderPrice - entryPrice
            (profit - (orderPrice - entryPrice).absoluteValue * 0.00055) * toReduce
        } else {
            0.0
        }
    }

    fun getPositionValue(): Double = entryPrice * size

    override fun toString(): String {
        return "Position(entryPrice=$entryPrice, size=$size, trend=$trend, realizedPnL=$realizedPnL)"
    }
}
