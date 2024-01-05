package space.dawdawich.service

import space.dawdawich.integration.client.bybit.ByBitPrivateHttpClient
import space.dawdawich.integration.model.PairInfo
import kotlinx.coroutines.runBlocking
import mu.KLogger
import mu.KotlinLogging
import org.slf4j.MDC
import org.slf4j.event.Level
import org.springframework.data.domain.Pageable
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import space.dawdawich.client.ByBitWebSocketClient
import space.dawdawich.exception.ReduceOnlyRuleNotSatisfiedException
import space.dawdawich.repositories.GridTableAnalyzerRepository
import space.dawdawich.repositories.entity.constants.AnalyzerChooseStrategy
import space.dawdawich.repositories.entity.GridTableAnalyzerDocument
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.repositories.entity.constants.ManagerStatus
import space.dawdawich.service.helper.PositionManager
import space.dawdawich.service.model.Order
import space.dawdawich.service.model.Position
import space.dawdawich.utils.calculatePercentageChange
import space.dawdawich.utils.plusPercent
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.List
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.minutes

class TradeManager(
    private val tradeManagerData: TradeManagerDocument,
    private val priceTickerListenerFactoryService: PriceTickerListenerFactoryService,
    private val analyzerRepository: GridTableAnalyzerRepository,
    private val bybitService: ByBitPrivateHttpClient,
    private val managerService: TradeManagerService
) {
    private val _logger = KotlinLogging.logger {}
    private infix fun logger(action: (KLogger) -> Unit) {
        MDC.put("manager-id", tradeManagerData.id)
        action(_logger)
        MDC.clear()
    }


    private var priceListener: ConcurrentMessageListenerContainer<String, String>? = null
    lateinit var webSocketClient: ByBitWebSocketClient

    var analyzer: GridTableAnalyzerDocument? = null

    private var startCapital = 0.0
    private var capital = 0.0
    private lateinit var priceInstruction: PairInfo

    private var price: Double = -1.0
    private var orderPriceGrid: MutableMap<Double, Order?> = mutableMapOf()
    private var positionManager: PositionManager? = null

    private var analyzerUpdateTimestamp: Long = 0

    private val df = DecimalFormat("#").apply { maximumFractionDigits = 9 }

    var middlePrice: Double = -1.0

    init {
        if (tradeManagerData.status == ManagerStatus.ACTIVE) {
            setupAnalyzer()
            updateCapital()
            startCapital = capital
        }
    }

    private fun updateCapital() {
        capital = runBlocking { bybitService.getAccountBalance() }
        logger { it.info { "Capital is: $capital" } }
    }

    fun updatePosition(position: List<Position>) {
        positionManager?.updatePosition(position)
    }

    fun updateOrder(order: Order) {
        logger { it.info { "Obtained order to update; id: ${order.orderLinkId}, status: ${order.orderStatus}" } }
        orderPriceGrid.entries.firstOrNull { it.value?.orderLinkId == order.orderLinkId }?.key?.apply {
            orderPriceGrid[this] = order
            logger { it.info { "Updated order in store with: $order" } }
        }
    }

    private fun updatePrice(newPrice: Double) {
        if (tradeManagerData.status == ManagerStatus.ACTIVE) {
            if (analyzer != null && analyzer!!.middlePrice != null) {
                if (price <= 0) {
                    middlePrice = analyzer!!.middlePrice!!
                    setUpPrices()
                }

                price = newPrice

                checkOrders()

                positionManager?.getPositions()?.filter { position ->
                    if (position.size > 0.0) {
                        val result = capital + position.calculateProfit(price)
                        return@filter result > capital.plusPercent(analyzer!!.positionTakeProfit) || result < capital.plusPercent(
                            -analyzer!!.positionStopLoss
                        )
                    }
                    return@filter false
                }?.forEach {
                    logger { it.info { "Cancel position. SL/TP exited. $it" } }
                    closePosition(it)
                }
            } else {
                analyzer = null
            }
        }
        findNewAnalyzer()
    }

    private fun setUpPrices() {
        var minPrice = middlePrice
        var maxPrice = middlePrice
        val step =
            (middlePrice.plusPercent(analyzer!!.diapason) - middlePrice.plusPercent(-analyzer!!.diapason)) / analyzer!!.gridSize

        val gridPrices = mutableListOf<Double>()
        minPrice -= step
        repeat(analyzer!!.gridSize / 2) {
            minPrice -= step
            gridPrices += minPrice
        }
        maxPrice += step
        repeat(analyzer!!.gridSize / 2) {
            maxPrice += step
            gridPrices += maxPrice
        }
        orderPriceGrid = mutableMapOf(*gridPrices.map { it to null }.toTypedArray())
    }

    private fun closePosition(position: Position) {
        try {
            runBlocking {
                bybitService.closePosition(
                    position.symbol,
                    position.isLong,
                    position.size,
                    position.positionIdx
                )
            }
        } catch (ex: ReduceOnlyRuleNotSatisfiedException) {
            positionManager =
                PositionManager(runBlocking {
                    bybitService.getPositionInfo(analyzer!!.symbolInfo.symbol).map {
                        Position(
                            it.symbol,
                            it.isLong,
                            it.size,
                            it.entryPrice,
                            it.positionIdx,
                            it.updateTime
                        )
                    }
                })
        }

        updateCapital()
        val percentChange = startCapital.calculatePercentageChange(capital)
        tradeManagerData.stopLoss?.let {
            if (percentChange < -it) {
                managerService.deactivateTradeManager(tradeManagerData.id, status = ManagerStatus.INACTIVE, stopDescription = "Stop Loss exceeded")
            }
        }
        tradeManagerData.takeProfit?.let {
            if (percentChange > it) {
                managerService.deactivateTradeManager(tradeManagerData.id, status = ManagerStatus.INACTIVE, stopDescription = "Take Profit exceeded")
            }
        }
        logger { it.info { "Updated capital: $capital" } }
    }

    private fun checkOrders() {
        val nearOrders = orderPriceGrid.entries.filter { (it.key - price).absoluteValue > priceInstruction.tickSize }
            .sortedBy { (it.key - price).absoluteValue }.take(2)

        nearOrders.filter { it.value == null }.forEach {
            val moneyPerPosition = capital / analyzer!!.gridSize

            val isLong = it.key < middlePrice
            val regexToSplit = "[.,]".toRegex()
            val floatNumberLength =
                if (priceInstruction.tickSize != 1.0) df.format(priceInstruction.tickSize)
                    .split(regexToSplit)[1].length else 0
            val inPrice = BigDecimal(it.key).setScale(
                floatNumberLength,
                RoundingMode.HALF_DOWN
            ).toDouble()

            val length = if (priceInstruction.minOrderQty != 1.0) df.format(priceInstruction.minOrderQty)
                .split(regexToSplit)[1].length else 0
            val qty = BigDecimal(moneyPerPosition * analyzer!!.multiplayer / inPrice).setScale(
                length, RoundingMode.HALF_DOWN
            ).toDouble()

            if (qty <= 0.0 || (positionManager!!.getPositionsValue() / analyzer!!.multiplayer) + 0.1 > capital) {
                return@forEach
            }

            if (positionManager!!.isOneWay()) {
                positionManager!!.getPositions().firstOrNull { it.isLong != isLong && it.size > 0.0 }?.let { pos ->
                    val prof = if (pos.isLong) it.key - pos.entryPrice else pos.entryPrice - it.key
                    (prof - pos.entryPrice * 0.00055 - it.key * 0.00055) * qty > 0
                }
            } else {
                null
            }?.let { higherThanZero -> if (!higherThanZero) return@forEach }

            // create order
            val symbol = analyzer!!.symbolInfo.symbol
            val orderId: String = UUID.randomUUID().toString()
            val result = runBlocking {
                bybitService.createOrder(
                    symbol,
                    inPrice,
                    qty,
                    isLong,
                    positionManager!!.getPositionIdx(isLong),
                    orderId,
                    if (price > inPrice) 2 else 1
                )
            }

            if (result) {
                orderPriceGrid[it.key] = Order(symbol, isLong, it.key, qty, "Untriggered", orderId)
                logger { it.info { "Added order to store id: $orderId" } }
            } else {
                logger { it.warn { "FAILED TO CREATE ORDER" } }
            }
        }

        orderPriceGrid.entries.filter { it.value != null }.forEach { pair ->
            val status = pair.value!!.orderStatus
            if (status.equals("Filled", true) || status.equals("Deactivated", true) || status.equals(
                    "Rejected",
                    true
                )
            ) {
                val minPrice = middlePrice.plusPercent(-analyzer!!.diapason)
                val maxPrice = middlePrice.plusPercent(analyzer!!.diapason)
                val step = (maxPrice - minPrice) / analyzer!!.gridSize
                if ((pair.value!!.price - price).absoluteValue > step) {
                    logger { it.info { "Order at price ${pair.key} reopened for interact" } }
                    orderPriceGrid[pair.key] = null
                }
            }
        }
    }

    fun updateMiddlePrice(middlePrice: Double) {
        closeAllPositionsAndOrders()
        this.middlePrice = middlePrice
        logger { it.info { "Middle price updated" } }
    }

    private fun closeAllPositionsAndOrders() {
        analyzer?.let { analyzer ->
            runBlocking { bybitService.cancelAllOrder(analyzer.symbolInfo.symbol) }
            try {
                positionManager?.getPositions()?.filter { it.size > 0.0 }?.forEach { position ->
                    runBlocking {
                        bybitService.closePosition(
                            position.symbol,
                            position.isLong,
                            position.size,
                            position.positionIdx
                        )
                    }
                }
            } catch (ex: ReduceOnlyRuleNotSatisfiedException) {
                positionManager =
                    PositionManager(runBlocking {
                        bybitService.getPositionInfo(analyzer!!.symbolInfo.symbol).map {
                            Position(
                                it.symbol,
                                it.isLong,
                                it.size,
                                it.entryPrice,
                                it.positionIdx,
                                it.updateTime
                            )
                        }
                    })
            }
        }
    }

    private fun setupAnalyzer() {
        logger { it.info { "Trade manager setup: $tradeManagerData" } }
        if (tradeManagerData.status == ManagerStatus.ACTIVE) {
            if (tradeManagerData.chooseStrategy == AnalyzerChooseStrategy.CUSTOM && tradeManagerData.customAnalyzerId.isNotBlank()) {
                analyzer = analyzerRepository.findById(tradeManagerData.customAnalyzerId).get()
            } else if (tradeManagerData.chooseStrategy == AnalyzerChooseStrategy.BIGGEST_BY_MONEY) {
                analyzer = analyzerRepository.findAllByOrderByMoneyDesc(Pageable.ofSize(1)).get().toList()[0]
            }
            if (analyzer != null && analyzer!!.middlePrice != null) {
                logger { log -> log.info { "Assigned analyzer to manager '${analyzer!!.id}'" } }
                runBlocking {
                    bybitService.setMarginMultiplier(analyzer!!.symbolInfo.symbol, analyzer!!.multiplayer, 3)
                }
                priceInstruction = runBlocking { bybitService.getPairInstructions(analyzer!!.symbolInfo.symbol) }
                positionManager =
                    PositionManager(runBlocking {
                        bybitService.getPositionInfo(analyzer!!.symbolInfo.symbol).map {
                            Position(
                                it.symbol,
                                it.isLong,
                                it.size,
                                it.entryPrice,
                                it.positionIdx,
                                it.updateTime
                            )
                        }
                    })


                priceListener?.stop()
                priceListener = priceTickerListenerFactoryService.getPriceListener(
                    analyzer!!.symbolInfo.symbol,
                    analyzer!!.symbolInfo.testServer
                )
                priceListener!!.setupMessageListener(MessageListener<String, String> {
                    try {
                        val newPrice = it.value().toDouble()
                        logger { log -> log.info { "Manager accepts price: $newPrice; symbol: ${analyzer!!.symbolInfo.symbol}" } }
                        updatePrice(newPrice)
                    } catch (ex: Exception) {
                        logger { log -> log.error(ex) { "Failed to update manager price." } }
                        managerService.deactivateTradeManager(tradeManagerData.id, ex = ex)
                    }
                })
                priceListener!!.start()
                logger { log -> log.info { "Price listener started." } }
            } else {
                analyzer = null
            }
        }
    }

    private fun findNewAnalyzer() {
        if (analyzer == null || analyzerUpdateTimestamp < System.currentTimeMillis()) {
            analyzerUpdateTimestamp = System.currentTimeMillis() + 10.minutes.inWholeMilliseconds
            val biggestAnalyzers = analyzerRepository.findAllByOrderByMoneyDesc(Pageable.ofSize(50)).get().toList()
            val biggestValue = biggestAnalyzers.maxBy { it.money }

            if (biggestAnalyzers.filter { it.money == biggestValue.money }.none { it.id == analyzer?.id }) {
                closeAllPositionsAndOrders()
                setupAnalyzer()
                middlePrice = analyzer!!.middlePrice!!
                setUpPrices()
            }
        }
    }

    fun deactivateManager() {
        try {
            closeAllPositionsAndOrders()
            webSocketClient.close()
            if (priceListener?.isRunning == true) {
                priceListener?.stop()
            }
            priceListener = null
        } catch (ex: Exception) {
            logger { it.error(ex) { "Error: Failed to deactivate manager" } }
        }
    }

    fun getId() = tradeManagerData.id
}
