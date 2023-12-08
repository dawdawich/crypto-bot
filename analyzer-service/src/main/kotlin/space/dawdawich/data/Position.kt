package space.dawdawich.data

import space.dawdawich.utils.plusPercent
import java.util.*

class Position(
    var entryPrice: Double,
    var size: Double,
    val trend: Trend,
    val id: String = UUID.randomUUID().toString()
) {
    var realizedPnL = 0.0

    fun updateSizeAndEntryPrice(order: Order) {
        if (order.trend == trend) {
            entryPrice = ((entryPrice * size) + (order.inPrice * order.count)) / (size + order.count)
            size += order.count
            realizedPnL -= order.inPrice * 0.00055 * size
        } else {
            size -= order.count
            val profit = if (order.trend == Trend.SHORT) order.inPrice - entryPrice else entryPrice - order.inPrice
            realizedPnL += (profit - order.inPrice * 0.00055) * size
        }
    }

    fun isTpOrSlCrossed(currentPrice: Double, sl: Double, tp: Double): Boolean {
        val tpPrice = entryPrice.plusPercent(tp * trend.direction)
        val slPrice = entryPrice.plusPercent(-sl * trend.direction)


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
        return (profitPerUnit - (currentPrice - entryPrice) * 0.00055) * size + realizedPnL
    }
}
