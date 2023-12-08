package space.dawdawich.analyzers.helper

import space.dawdawich.data.Order
import space.dawdawich.data.Position
import space.dawdawich.data.Trend


class PositionManager(private val isOneWay: Boolean, private val stopLoss: Double, private val takeProfit: Double) {
    private val positions: MutableSet<Position> = mutableSetOf(
        Position(0.0, 0.0, Trend.LONG),
        Position(0.0, 0.0, Trend.SHORT)
    )

    fun updatePosition(order: Order) {
        val position = if (isOneWay) {
            positions.find { it.size > 0.0 } ?: positions.first { it.trend == order.trend }
        } else {
            positions.first { it.trend == order.trend }
        }
        position.updateSizeAndEntryPrice(order)
    }

    fun checkPositions(price: Double, exceedFunction: (Position) -> Unit = { _ -> }): Boolean {
        var result = false
        positions.filter { it.size > 0.0 && it.isTpOrSlCrossed(price, stopLoss, takeProfit) }.forEach {
            result = true
            exceedFunction(it)
        }
        return result
    }

    fun resetPositions(price: Double, trend: Trend): Double {
        if (isOneWay) {
            val profit = positions.map { it.calculateProfit(price) }.sumOf { it }

            positions.forEach {
                it.size = 0.0
                it.entryPrice = 0.0
                it.realizedPnL = 0.0
            }
            return profit
        } else {
            val profit = positions.filter { it.trend == trend }.map { it.calculateProfit(price) }.sumOf { it }
            positions.filter { it.trend == trend }.forEach {
                it.size = 0.0
                it.entryPrice = 0.0
                it.realizedPnL = 0.0
            }
            return profit
        }
    }

    fun getPositionsValue(): Double {
        return positions.sumOf {
            it.size * it.entryPrice
        }
    }

    fun isOneWay() = isOneWay

    fun getPositions() = positions
}
