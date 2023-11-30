package space.dawdawich.data

import space.dawdawich.utils.plusPercent
import java.util.*

class Position(var entryPrice: Double, var size: Double, val trend: Trend, private val takeProfit: Double, private val stopLoss: Double, val id: String = UUID.randomUUID().toString()) {
    private val orders = mutableListOf<Order>()
    var closePrice: Double? = null

    constructor(order: Order, takeProfit: Double, stopLoss: Double) :
            this(order.inPrice, order.count, order.trend, takeProfit, stopLoss) {
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
            Trend.LONG -> {
                currentPrice >= tpPrice
            }

            else -> {
                currentPrice <= tpPrice
            }
        }

        val isSlCrossed = when (trend) {
            Trend.LONG -> {
                currentPrice <= slPrice
            }

            else -> {
                currentPrice >= slPrice
            }
        }

        return isTpCrossed || isSlCrossed
    }

    fun calculateProfit(currentPrice: Double): Double {
        val profitPerUnit = if (trend == Trend.SHORT) currentPrice - entryPrice else entryPrice - currentPrice
        return (profitPerUnit - currentPrice * 0.00055) * size
    }
}
