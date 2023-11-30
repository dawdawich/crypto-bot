package space.dawdawich.cryptobot.analyzer

import space.dawdawich.cryptobot.client.BybitTickerWebSocketClient
import space.dawdawich.cryptobot.data.*
import space.dawdawich.cryptobot.interfaces.AnalyzerInterface
import space.dawdawich.cryptobot.manager.OrderManager
import space.dawdawich.cryptobot.pairInstructions
import space.dawdawich.cryptobot.pairMinPriceInstructions
import space.dawdawich.cryptobot.util.calculatePercentageChange
import space.dawdawich.cryptobot.util.plusPercent
import space.dawdawich.cryptobot.util.step
import java.util.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class GridTableAnalyzer(
    val diapason: Int,
    val gridSize: Int,
    var money: Double,
    val multiplier: Int,
    val stopLoss: Int,
    val takeProfit: Int,
    val pair: String,
    val id: String = UUID.randomUUID().toString()
) : AnalyzerInterface {
    val orders: MutableList<Order> = mutableListOf()
    var longPosition: Position? = null
    var shortPosition: Position? = null

    var currentPrice: Double = -1.0
    var previousPrice: Double = -1.0

    var minPrice: Double = -1.0
    var maxPrice: Double = -1.0
    var diapasonTimestamp = System.currentTimeMillis()
    var pnlTimestamp = System.currentTimeMillis()
    var iterationCounter = 0L
    var pnlFactor: Double = 0.0
    var outOfBoundCounter = 0
    var crossMiddleCounter = 0

    private var previousMoney: Double = money

    fun calculateMoneyWithPositions(): Double =
        money + (longPosition?.calculateProfit(currentPrice) ?: 0.0) + (shortPosition?.calculateProfit(currentPrice)
            ?: 0.0)

    override fun accept(t: Double) {
        if (minPrice <= 0.0) {
            currentPrice = t
            setUpPrices()
            previousPrice = currentPrice
            return
        }

        previousPrice = currentPrice
        currentPrice = t

        tryToCalculatePnL()
        if (longPosition == null && shortPosition == null && currentPrice !in minPrice..maxPrice) {
            if (outOfBoundCounter++ > 100) {
                resetOrders()
            }
            return
        }
        outOfBoundCounter = 0

        if ((minPrice + maxPrice) / 2 in previousPrice..currentPrice) {
            crossMiddleCounter = 0
        } else if (longPosition == null && shortPosition == null && crossMiddleCounter++ > 1500) {
            resetOrders()
            return
        }

        reinitializeBounds()


        processOrders()
        processPositions()
    }

    private fun reinitializeBounds(force: Boolean = false) {
        val curr = System.currentTimeMillis()
        if (force || curr - diapasonTimestamp > 3.hours.inWholeMilliseconds) {
            resetPositions()
            orders.forEach {
                if (it.isFilled) {
                    it.isFilled = false
                }
            }

            setUpPrices()
            diapasonTimestamp = curr
        }
    }

    private fun tryToCalculatePnL() {
        val curr = System.currentTimeMillis()
        if (curr - pnlTimestamp > 30.seconds.inWholeMilliseconds) {
            val calculatedBalance = calculateMoneyWithPositions()
            val pnl = if (previousMoney > 0.0) previousMoney.calculatePercentageChange(calculatedBalance) else 0.0
            pnlFactor = ((pnlFactor * iterationCounter) + pnl) / ++iterationCounter
            previousMoney = calculatedBalance
            pnlTimestamp = curr
        }
    }

    private fun setUpPrices() {
        minPrice = currentPrice.plusPercent(-diapason)
        maxPrice = currentPrice.plusPercent(diapason)
        setupGrid()
    }

    fun processOrders() {
        orders.forEach { order ->
            if (!order.isFilled) {
                order.isFilled =
                    (order.trend == Trend.BULL && order.inPrice < currentPrice && order.inPrice >= previousPrice) ||
                            (order.trend == Trend.BEAR && order.inPrice > currentPrice && order.inPrice <= previousPrice)
                if (order.isFilled) {
                    val orderToAdd = order.copy()
                    if (order.trend == Trend.BEAR) {
                        longPosition?.updateSizeAndEntryPrice(orderToAdd) ?: run {
                            longPosition = Position(orderToAdd, takeProfit.toDouble() / multiplier, stopLoss.toDouble() / multiplier)
                        }
                    } else {
                        shortPosition?.updateSizeAndEntryPrice(orderToAdd) ?: run {
                            shortPosition = Position(orderToAdd, takeProfit.toDouble() / multiplier, stopLoss.toDouble() / multiplier)
                        }
                    }
                }
            } else if (order.isTakeProfitExceeded(currentPrice) || order.isStopLossExceeded(currentPrice)) {
                order.isFilled = false
            }
        }
    }

    private fun processPositions() {
        longPosition?.let { long ->
            if (long.isTpOrSlCrossed(currentPrice)) {
                money += long.calculateProfit(currentPrice)
                longPosition = null
            }
        }
        shortPosition?.let { short ->
            if (short.isTpOrSlCrossed(currentPrice)) {
                money += short.calculateProfit(currentPrice)
                shortPosition = null
            }
        }
    }

    fun setupGrid() {
        orders.clear()
        val step = (maxPrice - minPrice) / gridSize
        val moneyPerPosition = money / gridSize
        val averagePrice = (maxPrice + minPrice) / 2

        for (price in minPrice..maxPrice - step step step) {
            val trend = if (price <= averagePrice) Trend.BULL else Trend.BEAR
            val qty = moneyPerPosition * multiplier / price

            if (qty < pairInstructions[pair]!! || step < pairMinPriceInstructions[pair]!!.toDouble()) {
                BybitTickerWebSocketClient.instance.removeSubscriber(pair, this)
                money = 0.0
                orders.clear()
                return
            }

            orders += Order(
                price,
                pair,
                qty,
                price - step * trend.direction,
                price + step * trend.direction,
                trend
            )
        }
    }

    fun resetOrders() {
        resetPositions()
        orders.filter { it.isFilled }.forEach {
            it.isFilled = false
        }
        orders.clear()
        reinitializeBounds(true)
    }

    private fun resetPositions() {
        longPosition?.let { long ->
            money += long.calculateProfit(currentPrice)
            longPosition = null
        }
        shortPosition?.let { short ->
            money += short.calculateProfit(currentPrice)
            shortPosition = null
        }
    }


    override fun getAnalyzerInfo(): AnalyzerData {
        TODO("Not yet implemented")
    }

    override fun getInfoForOrder(): OpenOrderInfo {
        TODO("Not yet implemented")
    }

    override fun setupManager(manager: OrderManager?) {
        TODO("Not yet implemented")
    }

    override fun terminateOrder() {
        TODO("Not yet implemented")
    }

    override fun hasManager(): Boolean {
        return false
    }

    override fun toString(): String {
        return "GridTableAnalyzer(diapason=$diapason, gridSize=$gridSize, money=$money, multiplier=$multiplier, stopLoss=$stopLoss, pair='$pair', id='$id', minPrice=$minPrice, maxPrice=$maxPrice, pnl=$pnlFactor)"
    }


}
