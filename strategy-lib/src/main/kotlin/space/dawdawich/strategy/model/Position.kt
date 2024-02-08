package space.dawdawich.strategy.model

class Position(
    var entryPrice: Double,
    var size: Double,
    val trend: Trend,
    realizedPnL: Double? = null,
) {
    private var realizedPnL = -(entryPrice * 0.00055 * size)
    var outOfMoney = false

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
            val profit = if (order.trend == Trend.SHORT) order.inPrice - entryPrice else entryPrice - order.inPrice
            realizedPnL += (profit - order.inPrice * 0.00055) * toReduce
        }
    }

    fun calculateProfit(currentPrice: Double): Double {
        val profitPerUnit = if (trend == Trend.SHORT) currentPrice - entryPrice else entryPrice - currentPrice
        return (profitPerUnit - (currentPrice - entryPrice) * 0.00055) * size + realizedPnL
    }

    fun calculateReduceOrder(orderPrice: Double, orderSize: Double, orderTrend: Trend): Double {
        return if (orderTrend != trend) {
            val toReduce = if (orderSize > size) size else orderSize
            val profit = if (orderTrend == Trend.SHORT) orderPrice - entryPrice else entryPrice - orderPrice
            (profit - orderPrice * 0.00055) * toReduce
        } else {
            0.0
        }
    }

    fun getPositionValue(): Double = entryPrice * size

    override fun toString(): String {
        return "Position(entryPrice=$entryPrice, size=$size, trend=$trend, realizedPnL=$realizedPnL)"
    }
}
