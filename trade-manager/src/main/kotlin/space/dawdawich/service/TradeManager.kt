package space.dawdawich.service

import space.dawdawich.integration.client.bybit.ByBitPrivateHttpClient
import space.dawdawich.integration.model.PairInfo
import kotlinx.coroutines.runBlocking
import org.springframework.data.domain.Pageable
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import space.dawdawich.client.ByBitWebSocketClient
import space.dawdawich.exception.ReduceOnlyRuleNotSatisfiedException
import space.dawdawich.repositories.GridTableAnalyzerRepository
import space.dawdawich.repositories.entity.AnalyzerChooseStrategy
import space.dawdawich.repositories.entity.GridTableAnalyzerDocument
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.service.helper.PositionManager
import space.dawdawich.service.model.Order
import space.dawdawich.service.model.Position
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
    private val bybitService: ByBitPrivateHttpClient
) {
    private var priceListener: ConcurrentMessageListenerContainer<String, String>? = null
    lateinit var webSocketClient: ByBitWebSocketClient

    var analyzer: GridTableAnalyzerDocument? = null

    private var capital = 0.0
    private lateinit var priceInstruction: PairInfo

    private var price: Double = -1.0
    private var orderPriceGrid: MutableMap<Double, Order?> = mutableMapOf()
    private var positionManager: PositionManager? = null

    private var analyzerUpdateTimestamp: Long = 0

    private val df = DecimalFormat("#").apply { maximumFractionDigits = 9 }

    var middlePrice: Double = -1.0

    init {
        if (tradeManagerData.isActive) {
            setupAnalyzer()
            updateCapital()
        }
    }

    private fun updateCapital() {
        capital = runBlocking { bybitService.getAccountBalance() }
        println("Capital is: $capital")
    }

    fun updatePosition(position: List<Position>) {
        positionManager?.updatePosition(position)
    }

    fun updateOrder(order: Order) {
        println("Obtained order to update; id: ${order.orderLinkId}, status: ${order.orderStatus}")
        orderPriceGrid.entries.firstOrNull { it.value?.orderLinkId == order.orderLinkId }?.key?.apply {
            orderPriceGrid[this] = order
            println("Updated order in store with: $order")
        }
    }

    private fun updatePrice(newPrice: Double) {
        if (tradeManagerData.isActive && analyzer != null) {
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
                closePosition(it)
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
        val calculateProfit = position.calculateProfit(price)

        val direction = if (position.isLong) 1 else -1
        val tpPrice = capital.plusPercent(analyzer!!.positionTakeProfit * direction)
        val slPrice = capital.plusPercent(-analyzer!!.positionStopLoss * direction)
        if (position.size > 0.0 && (calculateProfit + capital) !in slPrice..tpPrice
        ) {
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
            println("Cancel position. SL/TP exited. $position")
            updateCapital()
            println("New capital: $capital")
        }
    }

    private fun checkOrders() {
        val nearOrders = orderPriceGrid.entries.filter { (it.key - price).absoluteValue > priceInstruction.tickSize }
            .sortedBy { (it.key - price).absoluteValue }.take(2)

        nearOrders.filter { it.value == null }.forEach {
            val moneyPerPosition = capital / analyzer!!.gridSize

            val regexToSplit = "[.,]".toRegex()
            val isLong = it.key < middlePrice
            val floatNumberLength =
                if (priceInstruction.tickSize != 1.0) df.format(priceInstruction.tickSize).split(regexToSplit)[1].length else 0
            val inPrice = BigDecimal(it.key).setScale(
                floatNumberLength,
                RoundingMode.HALF_DOWN
            ).toDouble()

            val s = df.format(priceInstruction.minOrderQty).split(regexToSplit)[1]
            val length = if (s.endsWith("0")) 0 else s.length
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
                println("Added order to store id: $orderId")
            } else {
                println("FAILED TO CREATE ORDER")
            }
        }

        orderPriceGrid.entries.filter { it.value != null }.forEach {
            val status = it.value!!.orderStatus
            if (status.equals("Filled", true) || status.equals("Deactivated", true) || status.equals(
                    "Rejected",
                    true
                )
            ) {
                val minPrice = middlePrice.plusPercent(-analyzer!!.diapason)
                val maxPrice = middlePrice.plusPercent(analyzer!!.diapason)
                val step = (maxPrice - minPrice) / analyzer!!.gridSize
                if ((it.value!!.price - price).absoluteValue > step) {
                    println("Removed Order from store: ${it.key}")
                    orderPriceGrid[it.key] = null
                }
            }
        }
    }

    fun updateTradeData(incomeData: TradeManagerDocument) {
        if (tradeManagerData.updateTime < incomeData.updateTime) {
            if (tradeManagerData.isActive != incomeData.isActive) {
                updateManagerStatus()
            }
            if (tradeManagerData.chooseStrategy != incomeData.chooseStrategy) {
                updateManagerStrategy(incomeData.chooseStrategy)
            }
            if (tradeManagerData.customAnalyzerId != incomeData.customAnalyzerId) {
                updateManagerCustomAnalyzerId(incomeData.customAnalyzerId)
            }
        }
    }

    fun updateMiddlePrice(middlePrice: Double) {
        closeAllPositionsAndOrders()
        this.middlePrice = middlePrice
        println("Changed middle price")

    }

    private fun closeAllPositionsAndOrders() {
        runBlocking { bybitService.cancelAllOrder(analyzer!!.symbolInfo.symbol) }
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

    private fun updateManagerCustomAnalyzerId(customAnalyzerId: String) {
        tradeManagerData.customAnalyzerId = customAnalyzerId
    }

    private fun updateManagerStrategy(chooseStrategy: AnalyzerChooseStrategy) {
        tradeManagerData.chooseStrategy = chooseStrategy
    }

    private fun updateManagerStatus() {
        tradeManagerData.isActive = !tradeManagerData.isActive

        if (!tradeManagerData.isActive) {
            priceListener?.stop()
            priceListener = null
            analyzer = null
        } else {
            println("Start manager: '${tradeManagerData.id}'")
            setupAnalyzer()
        }
    }

    private fun setupAnalyzer() {
        if (tradeManagerData.isActive) {
            if (tradeManagerData.chooseStrategy == AnalyzerChooseStrategy.CUSTOM && tradeManagerData.customAnalyzerId.isNotBlank()) {
                analyzer = analyzerRepository.findById(tradeManagerData.customAnalyzerId).get()
            } else if (tradeManagerData.chooseStrategy == AnalyzerChooseStrategy.BIGGEST_BY_MONEY) {
                analyzer = analyzerRepository.findAllByOrderByMoneyDesc(Pageable.ofSize(1)).get().toList()[0]
            }
            if (analyzer != null) {
                runBlocking {
                    bybitService.setMarginMultiplier(analyzer!!.symbolInfo.symbol, analyzer!!.multiplayer)
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
            }

            priceListener?.stop()
            priceListener = priceTickerListenerFactoryService.getPriceListener(analyzer!!.symbolInfo.symbol, analyzer!!.symbolInfo.testServer)
            priceListener!!.setupMessageListener(MessageListener<String, String> {
                updatePrice(it.value().toDouble())
                println("Update price in manager '${tradeManagerData.id}'; price - $price")
            })
            priceListener!!.start()
        }
    }

    private fun findNewAnalyzer() {
        if (analyzerUpdateTimestamp < System.currentTimeMillis()) {
            analyzerUpdateTimestamp = System.currentTimeMillis() + 30.minutes.inWholeMilliseconds
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

    fun getId() = tradeManagerData.id
}
