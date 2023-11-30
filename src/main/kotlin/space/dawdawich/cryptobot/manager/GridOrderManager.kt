package space.dawdawich.cryptobot.manager

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import space.dawdawich.cryptobot.analyzer.GridTableAnalyzer
import space.dawdawich.cryptobot.client.BybitTickerWebSocketClient
import space.dawdawich.cryptobot.client.data.OrderResponse
import space.dawdawich.cryptobot.data.Order
import space.dawdawich.cryptobot.data.Trend
import space.dawdawich.cryptobot.interfaces.OrderUpdateConsumer
import space.dawdawich.cryptobot.pairInstructions
import space.dawdawich.cryptobot.pairMinPriceInstructions
import space.dawdawich.cryptobot.service.OrderManagerService
import space.dawdawich.cryptobot.service.reuests.OrderDataRequest
import space.dawdawich.cryptobot.service.reuests.ReduceOrderRequest
import space.dawdawich.cryptobot.startCapital
import space.dawdawich.cryptobot.util.json
import space.dawdawich.cryptobot.util.leaveTail
import space.dawdawich.cryptobot.util.plusPercent
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import java.util.function.Consumer
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.minutes


class GridOrderManager(
    val analyzers: List<GridTableAnalyzer>,
    var money: Double
) : Consumer<Double>, OrderUpdateConsumer {
    val logger = KotlinLogging.logger {}

    var analyzer: GridTableAnalyzer? = null
    val placedOrders: MutableList<Order> = mutableListOf()

    var currentPrice = 0.0

    var refreshTimeout: Long = 0
    var previousUpdate: Long = 0
    var isAnalyzerStableFactor = 0

    override fun process(order: OrderResponse) {
        logger.info { "Received order to process: $order" }
        if (placedOrders.any { it.id == order.id }) {
            when (order.orderStatus) {
                "Filled" -> {
                    logger.info { "Order '${order.id}' was filled" }
                    val orderToProcess = placedOrders.first { it.id == order.id }
                    orderToProcess.isFilled = true
                    orderToProcess.inPrice = order.triggerPrice.toDouble()
                    orderToProcess.stopLoss = order.stopLoss.toDouble()
                    orderToProcess.takeProfit= order.takeProfit.toDouble()
                }

                "Rejected", "Cancelled", "Deactivated" -> {
                    logger.info { "Order '${order.id}' was canceled" }
                    placedOrders -= placedOrders.first { it.id == order.id }
                    analyzer?.let {
                        it.orders.first { it.id == order.id }.id = UUID.randomUUID().toString()
                    }
                }
            }
        }
    }

    @Synchronized
    override fun accept(t: Double) {
        currentPrice = t

        if (analyzer != null && isAnalyzerStableFactor > 5) {
            val ordersToRemove = placedOrders.filter {
                it.isFilled && it.pair == analyzer!!.pair && (it.isStopLossExceeded(currentPrice) || it.isTakeProfitExceeded(
                    currentPrice
                ))
            }.toMutableList()
                .onEach { logger.info { "Order '${it.id}' exceeded price bound. Price: $currentPrice, o sl: ${it.stopLoss}; o tp: ${it.takeProfit}" } }
            ordersToRemove += placedOrders.filter { it.pair != analyzer!!.pair }
                .onEach { logger.info { "Order '${it.id}' closed due to changed analyzer." } }

            if (ordersToRemove.isNotEmpty()) {
                placedOrders.removeAll(ordersToRemove)
                ordersToRemove.forEach { order ->
                    analyzer?.let {
                        it.orders.first { it.id == order.id }.id = UUID.randomUUID().toString()
                    }
                }
                money = runBlocking { OrderManagerService.getAccountBalance() }
                startCapital = money
            }
            getNearOrder(t).forEach { order ->
                if (placedOrders.none { it.id == order.id }) {
                    createOrder(order)
                }
            }
        }
    }

    suspend fun start() {
        while (true) {
            try {
                findAnalyzer()
                delay(1.minutes)
            } catch (e: Exception) {
                logger.info { "Failed to find analyzer: $e" }
            }
        }
    }

    private fun getNearOrder(price: Double): List<Order> {

        val sortedOrders = analyzer!!.orders.sortedBy { it.inPrice }
        val middleAnalyzer = sortedOrders.minBy { (it.inPrice - price).absoluteValue }
        val index = sortedOrders.indexOf(middleAnalyzer)

        if (middleAnalyzer.inPrice > price && index > 0) {
            return mutableListOf(sortedOrders[index], sortedOrders[index - 1])
        } else if (middleAnalyzer.inPrice > price && index < sortedOrders.size - 1) {
            return mutableListOf(sortedOrders[index], sortedOrders[index + 1])
        }
        return mutableListOf(sortedOrders[index])
    }

    private fun createOrder(order: Order) {
        val minQt = pairInstructions[analyzer!!.pair] ?: 0.1
        val priceQtyStep = pairMinPriceInstructions[analyzer!!.pair] ?: 0.1.toBigDecimal()
        val afterPoint = minQt.toString().split(".")[1]
        val afterPointQtyStep = priceQtyStep.toPlainString().trimEnd('0').split(".")[1]
        val numbersAfterPoint = if (afterPoint.length == 1 && afterPoint == "0") 0 else afterPoint.length
        val numbersAfterPointQtyStep =
            if (afterPointQtyStep.length == 1 && afterPointQtyStep == "0") 0 else afterPointQtyStep.length

        var inPrice = order.inPrice.leaveTail(numbersAfterPointQtyStep)
        var stopLoss = order.stopLoss.leaveTail(numbersAfterPointQtyStep)
        var takeProfit = order.takeProfit.leaveTail(numbersAfterPointQtyStep)

        val moneyPerPosition = money / analyzer!!.gridSize

        while (BigDecimal(inPrice).setScale(numbersAfterPointQtyStep, RoundingMode.HALF_UP) % priceQtyStep.setScale(
                numbersAfterPointQtyStep,
                RoundingMode.HALF_UP
            ) != BigDecimal.ZERO.setScale(numbersAfterPointQtyStep)
        ) {
            inPrice -= if (priceQtyStep.toDouble() > 1.0) 1.0 else priceQtyStep.toDouble()
            inPrice = inPrice.leaveTail(numbersAfterPoint)
        }

        while (BigDecimal(stopLoss).setScale(numbersAfterPointQtyStep, RoundingMode.HALF_UP) % priceQtyStep.setScale(
                numbersAfterPointQtyStep,
                RoundingMode.HALF_UP
            ) != BigDecimal.ZERO.setScale(numbersAfterPointQtyStep)
        ) {
            stopLoss -= if (priceQtyStep.toDouble() > 1) 1.0 else priceQtyStep.toDouble()
            stopLoss = stopLoss.leaveTail(numbersAfterPoint)
        }

        while (BigDecimal(takeProfit).setScale(numbersAfterPointQtyStep, RoundingMode.HALF_UP) % priceQtyStep.setScale(
                numbersAfterPointQtyStep,
                RoundingMode.HALF_UP
            ) != BigDecimal.ZERO.setScale(numbersAfterPointQtyStep)
        ) {
            takeProfit -= if (priceQtyStep.toDouble() > 1) 1.0 else priceQtyStep.toDouble()
            takeProfit = takeProfit.leaveTail(numbersAfterPoint)
        }

        var qty =
            (moneyPerPosition * analyzer!!.multiplier / inPrice).leaveTail(
                numbersAfterPoint
            )

        while (BigDecimal(qty).setScale(numbersAfterPoint, RoundingMode.HALF_UP) % BigDecimal(minQt).setScale(
                numbersAfterPoint,
                RoundingMode.HALF_UP
            ) != BigDecimal.ZERO.setScale(numbersAfterPoint)
        ) {
            qty -= if (minQt > 1) 1.0 else minQt
            qty = qty.leaveTail(numbersAfterPoint)
        }

        runBlocking {
            val positionIdx = when (analyzer!!.pair) {
                "GASUSDT", "CAKEUSDT", "TRBUSDT", "ARKUSDT", "ARBUSDT" -> 0
                else -> if (order.trend == Trend.BULL) 1 else 2
            }
            val orderToPlace = OrderDataRequest(
                order.pair,
                order.trend.directionName,
                "Market",
                qty.toString(),
                stopLoss.toString(),
                takeProfit.toString(),
                inPrice.toString(),
                positionIdx = positionIdx,
                triggerDirection = if (inPrice > currentPrice) 1 else 2,
                id = order.id
            ).apply { logger.info { "Order to create: $this" } }
            val orderId: String =
                OrderManagerService.createOrder(json.encodeToString(orderToPlace)) ?: return@runBlocking

            placedOrders += order.copy(
                id = orderId,
                inPrice = inPrice,
                count = qty,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                isFilled = false
            )
        }
    }

    private fun cancelAll() {
        if (analyzer != null) {
            runBlocking {
                OrderManagerService.cancelAllOrders(analyzer!!.pair)
                OrderManagerService.getActivePositionInfo(analyzer!!.pair).forEach {
                    if (it.size.toDouble() != 0.0) {
                        OrderManagerService.createOrder(
                            Json.encodeToString(
                                ReduceOrderRequest(
                                    analyzer!!.pair,
                                    if (it.side == Trend.BULL.directionName) Trend.BEAR.directionName else Trend.BULL.directionName,
                                    "Market",
                                    it.size,
                                    it.positionIdx
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    fun findAnalyzer() {
        val currTime = System.currentTimeMillis()
        if (currTime - previousUpdate > refreshTimeout) {
            val profitAnalyzer = analyzers.maxBy { it.pnlFactor }

            if (analyzer != profitAnalyzer && profitAnalyzer.money > money.plusPercent(1)) {
                cancelAll()
                analyzer?.let { BybitTickerWebSocketClient.instance.removeSubscriber(it.pair, this) }
                analyzer = profitAnalyzer
                BybitTickerWebSocketClient.instance.addSubscriber(analyzer!!.pair, this)
                runBlocking {
                    OrderManagerService.setMarginMultiplier(analyzer!!.pair, analyzer!!.multiplier)
                    money = OrderManagerService.getAccountBalance()
                    startCapital = money
                }
                isAnalyzerStableFactor = 0
                logger.info { "Analyzer successfully found: $analyzer" }
            } else if (analyzer == profitAnalyzer) {
                logger.info { "Current analyzer is good: $analyzer" }
                isAnalyzerStableFactor++
            }
            refreshTimeout = 1.minutes.inWholeMilliseconds
            previousUpdate = currTime
        }
    }
}
