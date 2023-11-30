package space.dawdawich.cryptobot.data

import space.dawdawich.cryptobot.util.plusPercent

class Position(order: Order, val takeProfit: Double, val stopLoss: Double) {
    private val orders = mutableListOf<Order>()
    var entryPrice: Double
    var size: Double
    var trend: Trend

    init {
        entryPrice = order.inPrice
        size = order.count
        trend = order.trend
        orders += order
    }

    fun updateSizeAndEntryPrice(order: Order) {
        if (!orders.contains(order)) {
            entryPrice = ((entryPrice * size) + (order.inPrice * order.count)) / (size + order.count)
            size += order.count
            orders += order
        }
    }

    fun isTpOrSlCrossed(currentPrice: Double): Boolean {
        val tpPrice = entryPrice.plusPercent(takeProfit * trend.direction)
        val slPrice = entryPrice.plusPercent(-stopLoss * trend.direction)



        val isTpCrossed = when (trend) {
            Trend.BULL -> {
                currentPrice >= tpPrice
            }
            else -> {
                currentPrice <= tpPrice
            }
        }

        val isSlCrossed = when (trend) {
            Trend.BULL -> {
                currentPrice <= slPrice
            }
            else -> {
                currentPrice >= slPrice
            }
        }

        return isTpCrossed || isSlCrossed
    }

    fun calculateProfit(currentPrice: Double): Double {
        val profitPerUnit = if (trend == Trend.BEAR) currentPrice - entryPrice else entryPrice - currentPrice
        return (profitPerUnit - currentPrice * 0.00055) * size
    }
}
