package space.dawdawich.cryptobot.analyzer

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import space.dawdawich.cryptobot.data.Order
import space.dawdawich.cryptobot.data.Trend
import space.dawdawich.cryptobot.manager.OrderManager
import space.dawdawich.cryptobot.util.plusPercent
import kotlin.math.abs

val logger = KotlinLogging.logger {}

val testCorrelator = 0.0
val orderTimeout = 30_000

class AutomaticSwitchTradingAnalyzer(
    wallet: Double = 100.0,
    stopLossPercent: Float,
    takeProfitPercent: Float,
    ticksToSwitch: Int = 1,
    trend: Trend = Trend.BULL,
    currentPrice: Double = 0.0,
    multiplier: Int = 1,
    pair: String
) : AnalyzerCore(wallet, stopLossPercent, takeProfitPercent, ticksToSwitch, trend, currentPrice, multiplier, pair) {

    override fun accept(t: Double) {
        previousPrice = currentPrice
        currentPrice = t

        if (currentPrice > previousPrice) {
            processHigherPrice()
        } else {
            processLowerPrice()
        }
    }

    private fun processLowerPrice() {
        if (trend == Trend.BULL) {
            order?.let { order ->
                if (order.isStopLossExceeded(currentPrice)) {
                    manager?.let {
                        logger.info { "ID: ${id}; Cross Stop Loss, close orders and positions. Current price: $currentPrice; stop loss: ${order.stopLoss};" }
                        runBlocking { it.cancelOrder() }
                        it.findProfitAnalyzer()
                    }
                    terminateOrder()
                    incrementSwitch()
                }
            } ?: run {
                incrementSwitch()
            }
        } else {
            order?.let { order ->
                if (order.isTakeProfitExceeded(currentPrice)) {
                    manager?.let {
                        logger.info { "ID: ${id}; Cross take profit, close orders and positions" }
                        runBlocking { it.cancelOrder() }
                        it.findProfitAnalyzer()
                    }
                    terminateOrder()
                } else if (stopLossRaiser < stopLossPercent - 1 && stopLossRaiser < -order.inPrice.calculatePercentageChangeWithMultiplier()) {
                    order.stopLoss = order.inPrice.plusPercent((stopLossPercent - ++stopLossRaiser) / multiplier)
                }

                if (!order.isFilled && order.inPrice >= currentPrice) {
                    order.isFilled = true
                } else if (!order.isFilled && System.currentTimeMillis() - order.createTime > orderTimeout) {
                    terminateOrder()
                    manager?.let {
                        logger.info { "ID: ${id}; Order timeout, cancel all" }
                        runBlocking { it.cancelOrder() }
                        it.findProfitAnalyzer()
                    }
                }
            } ?: run {
                val orderPrice = currentPrice.plusPercent(-0.1)
                manager?.let {
                    logger.info { "ID: ${id}; try to create order on pair '$pair' with current price '$currentPrice' and order price '$orderPrice'" }
                    runBlocking { it.createOrder() }
                    it.order?.let { managerOrder ->
                        logger.info { "ID: ${id}; Order successfully created, place it to the analyzer" }
                        order = Order(managerOrder.openPositionPrice, managerOrder.pair, managerOrder.qty, managerOrder.stopLoss, managerOrder.takeProfit, managerOrder.trend)
                    } ?: run { logger.info { "ID: ${id}; Order failed" } }
                } ?: run {
                    order = Order(
                        orderPrice,
                        pair,
                        wallet * multiplier / orderPrice,
                        orderPrice.plusPercent((stopLossPercent + testCorrelator) / multiplier),
                        orderPrice.plusPercent((-takeProfitPercent - testCorrelator) / multiplier),
                        trend
                    )
                }
            }
            switchCounter = 0
        }
    }

    private fun processHigherPrice() {
        if (trend == Trend.BEAR) {
            order?.let { order ->
                if (order.isStopLossExceeded(currentPrice)) {
                    manager?.let {
                        logger.info { "ID: ${id}; Cross Stop Loss, close orders and positions" }
                        runBlocking { it.cancelOrder() }
                        it.findProfitAnalyzer()
                    }
                    terminateOrder()
                    incrementSwitch()
                }
            } ?: run {
                incrementSwitch()
            }
        } else {
            order?.let { order ->
                if (order.isTakeProfitExceeded(currentPrice)) {
                    manager?.let {
                        logger.info { "ID: ${id}; Cross take profit, close orders and positions. Current price: $currentPrice; stop loss: ${order.stopLoss};" }
                        runBlocking { it.cancelOrder() }
                        it.findProfitAnalyzer()
                    }
                    terminateOrder()
                } else if (stopLossRaiser < stopLossPercent - 1 && stopLossRaiser < order.inPrice.calculatePercentageChangeWithMultiplier()) {
                    order.stopLoss = order.inPrice.plusPercent(-(stopLossPercent + ++stopLossRaiser) / multiplier)
                }

                if (!order.isFilled && order.inPrice <= currentPrice) {
                    order.isFilled = true
                } else if (!order.isFilled && System.currentTimeMillis() - order.createTime > orderTimeout) {
                    terminateOrder()
                    manager?.let {
                        logger.info { "ID: ${id}; Order timeout, cancel all" }
                        runBlocking { it.cancelOrder() }
                        it.findProfitAnalyzer()
                    }
                }
            } ?: run {
                val orderPrice = currentPrice.plusPercent(0.1)
                manager?.let {
                    logger.info { "ID: ${id}; try to create order on pair '$pair' with current price '$currentPrice' and order price '$orderPrice'" }
                    runBlocking { it.createOrder() }
                    it.order?.let { managerOrder ->
                        logger.info { "ID: ${id}; Order successfully created, place it to the analyzer" }
                        order = Order(managerOrder.openPositionPrice, managerOrder.pair, managerOrder.qty, managerOrder.stopLoss, managerOrder.takeProfit, managerOrder.trend)
                    } ?: run { logger.info { "ID: ${id}; Order failed" } }
                } ?: run {
                    order = Order(
                        orderPrice,
                        pair,
                        wallet * multiplier / orderPrice,
                        orderPrice.plusPercent((-stopLossPercent - testCorrelator) / multiplier),
                        orderPrice.plusPercent((takeProfitPercent + testCorrelator) / multiplier),
                        trend
                    )
                }
            }
            switchCounter = 0
        }
    }

    override fun terminateOrder() {
        order?.let {
            if (it.isFilled) {
                val profit = it.calculateProfit(currentPrice)
                if (profit > 0) {
                    profitOrdersCount++
                } else {
                    loseOrdersCount++
                }
                wallet += profit
            }
        }
        this.order = null
    }

    private fun incrementSwitch() {
        if (++switchCounter >= ticksToSwitch) {
            trend = if (trend == Trend.BULL) Trend.BEAR else Trend.BULL
            switchCounter = 0
        }
    }

    private fun Double.calculatePercentageChangeWithMultiplier(): Double {
        return ((currentPrice - this) / abs(this)) * 100.0 * multiplier
    }
}
