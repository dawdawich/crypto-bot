package space.dawdawich.analyzers

import java.util.*
import space.dawdawich.data.Order
import space.dawdawich.data.Position
import space.dawdawich.data.Trend
import space.dawdawich.utils.plusPercent
import space.dawdawich.utils.step
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class GridTableAnalyzer(
    val diapason: Int,
    val gridSize: Int,
    money: Double,
    val multiplier: Int,
    val stopLoss: Int,
    val takeProfit: Int,
    val symbol: String,
    val id: String = UUID.randomUUID().toString(),
    moneyChangeFunction: (KProperty<*>, Double, Double) -> Unit = {_,_,_->},
    positionChangeFunction: (KProperty<*>, Position?, Position?) -> Unit = {_,_,_->}
) {
    private val orders: MutableList<Order> = mutableListOf()

    private var minPrice: Double = -1.0
    private var maxPrice: Double = -1.0
    private var outOfBoundCounter = 0
    private var crossMiddleCounter = 0
    private var longPosition: Position? by Delegates.observable(null, positionChangeFunction)
    private var shortPosition: Position? by Delegates.observable(null, positionChangeFunction)

    var money: Double by Delegates.observable(money, moneyChangeFunction)


    fun acceptPriceChange(previousPrice: Double, currentPrice: Double) {
        if (minPrice <= 0.0) {
            setUpPrices(currentPrice)
            return
        }

        checkPriceForResetOrders(currentPrice, previousPrice)

        processPositions(currentPrice)
        processOrders(currentPrice, previousPrice)
    }

    private fun checkPriceForResetOrders(currentPrice: Double, previousPrice: Double) {
        if (longPosition == null && shortPosition == null && currentPrice !in minPrice..maxPrice && outOfBoundCounter++ > 100) {
            reinitializeBounds(currentPrice)
            return
        }
        outOfBoundCounter = 0

        val priceChangeRange =
            if (currentPrice > previousPrice) previousPrice..currentPrice else currentPrice..previousPrice
        if ((minPrice + maxPrice) / 2 in priceChangeRange) {
            crossMiddleCounter = 0
        } else if (longPosition == null && shortPosition == null && crossMiddleCounter++ > 1500) {
            reinitializeBounds(currentPrice)
        }
    }

    private fun reinitializeBounds(currentPrice: Double) {
        processPositions(currentPrice, true)
        orders.forEach {
            if (it.isFilled) {
                it.isFilled = false
            }
        }

        setUpPrices(currentPrice)
    }

    private fun setUpPrices(currentPrice: Double) {
        minPrice = currentPrice.plusPercent(-diapason)
        maxPrice = currentPrice.plusPercent(diapason)
        setupGrid()
    }

    private fun processOrders(currentPrice: Double, previousPrice: Double) {
        orders.forEach { order ->
            if (!order.isFilled) {
                order.isFilled =
                    (order.trend == Trend.LONG && order.inPrice < currentPrice && order.inPrice >= previousPrice) ||
                            (order.trend == Trend.SHORT && order.inPrice > currentPrice && order.inPrice <= previousPrice)
                if (order.isFilled) {
                    val orderToAdd = order.copy(count = money / gridSize * multiplier / order.inPrice) // apply price regarding current balance
                    if (order.trend == Trend.LONG) {
                        longPosition?.updateSizeAndEntryPrice(orderToAdd) ?: run {
                            longPosition = Position(
                                orderToAdd,
                                takeProfit.toDouble() / multiplier,
                                stopLoss.toDouble() / multiplier
                            )
                        }
                    } else {
                        shortPosition?.updateSizeAndEntryPrice(orderToAdd) ?: run {
                            shortPosition = Position(
                                orderToAdd,
                                takeProfit.toDouble() / multiplier,
                                stopLoss.toDouble() / multiplier
                            )
                        }
                    }
                }
            } else if (order.isTakeProfitExceeded(currentPrice) || order.isStopLossExceeded(currentPrice)) {
                order.isFilled = false
            }
        }
    }

    private fun processPositions(currentPrice: Double, skipLimitsCheck: Boolean = false) {
        longPosition?.let { long ->
            if (skipLimitsCheck || long.isTpOrSlCrossed(currentPrice)) {
                money += long.calculateProfit(currentPrice)
                long.closePrice = currentPrice
                longPosition = null
            }
        }
        shortPosition?.let { short ->
            if (skipLimitsCheck || short.isTpOrSlCrossed(currentPrice)) {
                money += short.calculateProfit(currentPrice)
                short.closePrice = currentPrice
                shortPosition = null
            }
        }
    }

    private fun setupGrid() {
        orders.clear()
        val step = (maxPrice - minPrice) / gridSize
        val moneyPerPosition = money / gridSize
        val averagePrice = (maxPrice + minPrice) / 2

        for (price in minPrice..maxPrice - step step step) {
            val trend = if (price <= averagePrice) Trend.LONG else Trend.SHORT
            val qty = moneyPerPosition * multiplier / price

//            if (qty < pairInstructions[pair]!! || step < pairMinPriceInstructions[pair]!!.toDouble()) {  TODO: extract it to separate checks before creating analyzer
//                BybitTickerWebSocketClient.instance.removeSubscriber(pair, this)
//                money = 0.0
//                orders.clear()
//                return
//            }

            orders += Order(
                price,
                symbol,
                qty,
                price - step * trend.direction,
                price + step * trend.direction,
                trend
            )
        }
    }
}
