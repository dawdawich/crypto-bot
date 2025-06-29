package space.dawdawich.strategy.model

import kotlin.math.absoluteValue

class Position(
    var entryPrice: Double,
    var size: Double,
    val trend: Trend,
    realizedPnL: Double? = null,
    val createTime: Long = System.currentTimeMillis()
) {
    private var realizedPnL = -(entryPrice * 0.00055 * size)

    init {
        realizedPnL?.let {
            this.realizedPnL = it
        }
    }


    fun calculateProfit(currentPrice: Double): Double {
        return calculateUnrealizedPnL(currentPrice) + realizedPnL
    }

    fun calculateROI(currentPrice: Double, leverage: Int): Double {
        val unrealizedPnL = calculateUnrealizedPnLInCoin(currentPrice);
        val initialMargin = size / (entryPrice * leverage)
        return (unrealizedPnL / initialMargin) * 100
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

    private fun calculateUnrealizedPnL(currentPrice: Double): Double {
        val profitPerUnit = if (trend.directionBoolean) currentPrice - entryPrice else entryPrice - currentPrice
        return (profitPerUnit - (currentPrice - entryPrice).absoluteValue * 0.00055) * size
    }

    private fun calculateUnrealizedPnLInCoin(currentPrice: Double): Double {
        return if (trend.directionBoolean) {
            size * ((1 / entryPrice) - (1 / currentPrice))
        } else {
            size * ((1 / currentPrice) - (1 / entryPrice))

        }
    }

    override fun toString(): String {
        return "Position(entryPrice=$entryPrice, size=$size, trend=$trend, realizedPnL=$realizedPnL)"
    }
}
