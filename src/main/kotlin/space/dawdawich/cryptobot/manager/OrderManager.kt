package space.dawdawich.cryptobot.manager

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import space.dawdawich.cryptobot.data.Trend
import space.dawdawich.cryptobot.interfaces.AnalyzerInterface
import space.dawdawich.cryptobot.manager.data.ActiveOrder
import space.dawdawich.cryptobot.pairInstructions
import space.dawdawich.cryptobot.pairMinPriceInstructions
import space.dawdawich.cryptobot.service.AnalyzersLeaderboard
import space.dawdawich.cryptobot.service.OrderManagerService
import space.dawdawich.cryptobot.service.reuests.OrderDataRequest
import space.dawdawich.cryptobot.service.reuests.ReduceOrderRequest
import space.dawdawich.cryptobot.util.calculatePercentageChange
import space.dawdawich.cryptobot.util.json
import space.dawdawich.cryptobot.util.leaveTail
import space.dawdawich.cryptobot.util.plusPercent
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

val logger = KotlinLogging.logger {}

class OrderManager(
    var money: Double,
    private val analyzersLeaderboard: AnalyzersLeaderboard
) {
    private var shouldClose = false
    private var analyzer: AnalyzerInterface? = null

    var order: ActiveOrder? = null

    suspend fun start() {
        while (true) {
            delay(15.seconds)
            println("tick")

            try {
                order?.let { order ->
                    if (OrderManagerService.isOrderDone(order.id)
                            .apply {
                                logger.info { "Is order '${order.id}' filled: $this" }
                                order.isFilled = this
                            } && checkPositionByOrder(order)
                    ) {
                        this.order = null
                        money = OrderManagerService.getAccountBalance()
                        analyzer?.terminateOrder()
                    }
                }

                if (shouldClose) {
                    return
                }
                if (order == null) {
                    findProfitAnalyzer()
                    analyzer?.let {
                        logger.info { "Terminate analyzer active order" }
                        it.terminateOrder()
                    }
                }
            } catch (e: Exception) {
                logger.error { "Weird Exception aquired: $e" }
            }
        }
    }

    private suspend fun checkPositionByOrder(order: ActiveOrder): Boolean {
        val position = OrderManagerService.hasNoActivePositions(order.pair, order.trend.directionName)
        if (position.first != 0.0) {
            order.watchdog += money.calculatePercentageChange(money + position.second)
        } else {
            return true
        }
        if (order.watchdog.absoluteValue > 100) {
            logger.info { "Cancel Active order cause of timeout" }
            cancelOrder()
            return true
        }
        return false
    }

    fun findProfitAnalyzer() {
        val profitAnalyzer = analyzersLeaderboard.getHigherPlaceByBalanceAndOrder(4)
        if (profitAnalyzer.getAnalyzerInfo().wallet > money.plusPercent(0.1) && profitAnalyzer != analyzer) {
            logger.info { """Switching to analyzer: id: ${profitAnalyzer.id}, m:  ${profitAnalyzer.multiplier}, tp: ${profitAnalyzer.takeProfitPercent}, sl: ${profitAnalyzer.stopLossPercent}, sw: ${profitAnalyzer.ticksToSwitch}""" }
            analyzer?.setupManager(null)
            analyzer = profitAnalyzer
            analyzer?.setupManager(this)
            analyzer?.terminateOrder()
            return
        }
    }

    suspend fun createOrder() {
        try {
            val readProfitAnalyzer = analyzer!!.getInfoForOrder()
            val pair = analyzer!!.getAnalyzerInfo().pair
            val side = readProfitAnalyzer.trend.directionName
            val direction = readProfitAnalyzer.trend.direction
            val positionInvests = money.plusPercent(-10)
            when {
                order == null -> {
                    val operationResult = OrderManagerService.setMarginMultiplier(pair, readProfitAnalyzer.multiplier)
                    if (!operationResult) {
                        logger.warn { "Failed to change multiplier" }
                    }
                    val minQt = pairInstructions[pair] ?: 0.1
                    val priceQtyStep = pairMinPriceInstructions[pair] ?: 0.1
                    val afterPoint = minQt.toString().split(".")[1]
                    val afterPointQtyStep = priceQtyStep.toString().split(".")[1]
                    val numbersAfterPoint = if (afterPoint.length == 1 && afterPoint == "0") 0 else afterPoint.length
                    val numbersAfterPointQtyStep =
                        if (afterPointQtyStep.length == 1 && afterPointQtyStep == "0") 0 else afterPointQtyStep.length
                    val inPrice = readProfitAnalyzer.currentPrice.plusPercent(0.1 * direction)
                    val (stopLoss, takeProfit) =
                        Pair(
                            inPrice.plusPercent(-readProfitAnalyzer.stopLoss * readProfitAnalyzer.trend.direction / readProfitAnalyzer.multiplier)
                                .leaveTail(numbersAfterPointQtyStep)
                                .toString(),
                            inPrice.plusPercent(readProfitAnalyzer.takeProfit * readProfitAnalyzer.trend.direction / readProfitAnalyzer.multiplier)
                                .leaveTail(numbersAfterPointQtyStep)
                                .toString()
                        )


                    var qty =
                        (positionInvests * readProfitAnalyzer.multiplier / inPrice).leaveTail(
                            numbersAfterPoint
                        )

                    while (qty % minQt != 0.0) {
                        qty -= if (minQt > 1) 1.0 else minQt
                        qty = qty.leaveTail(numbersAfterPoint)
                    }
                    OrderManagerService.createOrder(
                        json.encodeToString(
                            OrderDataRequest(
                                pair,
                                side,
                                "Market",
                                qty.toString(),
                                stopLoss,
                                inPrice.leaveTail(numbersAfterPointQtyStep).toString(),
                                inPrice.leaveTail(numbersAfterPointQtyStep).toString(), // TODO: fix
                                positionIdx = 0,
                                triggerDirection = if (readProfitAnalyzer.trend == Trend.BULL) 1 else 2
                            ).apply { logger.info { "Order to create: $this" } }
                        )
                    )?.let {
                        order = ActiveOrder(
                            it,
                            inPrice,
                            takeProfit.toDouble(),
                            readProfitAnalyzer.multiplier,
                            pair,
                            qty,
                            stopLoss.toDouble(),
                            trend = readProfitAnalyzer.trend,
                            false
                        )
                        logger.info { "Created New Order: '${order!!.id}'" }
                    } ?: run {
                        println("Failed to create order")
                    }
                }

                OrderManagerService.isOrderDone(order!!.id)
                    .apply {
                        logger.info { "Is order '${order!!.id}' filled: $this" }
                        order!!.isFilled = this
                    } && checkPositionByOrder(order!!) -> {
                    order = null
                    money = OrderManagerService.getAccountBalance()
                    analyzer?.terminateOrder()
                }
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    suspend fun cancelOrder() {
        if (order != null) {
            OrderManagerService.cancelAllOrders(order!!.pair)
            val activePositionInfo = OrderManagerService.getActivePositionInfo(
                order!!.pair)
            if (activePositionInfo.size.toDouble() > 0.0) {
                logger.info { "Try to cancel active position. Order ID ${order!!.id}" }
                OrderManagerService.createOrder(
                    Json.encodeToString(
                        ReduceOrderRequest(
                            order!!.pair,
                            if (order!!.trend == Trend.BULL) Trend.BEAR.directionName else Trend.BULL.directionName,
                            "Market",
                            activePositionInfo[0].size,
                            activePositionInfo[0].positionIdx
                        )
                    )
                )
            }
        }
    }
}
