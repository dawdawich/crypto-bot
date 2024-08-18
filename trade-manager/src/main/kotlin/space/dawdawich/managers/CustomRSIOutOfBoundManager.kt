package space.dawdawich.managers

import com.fasterxml.jackson.core.type.TypeReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.amqp.rabbit.listener.MessageListenerContainer
import space.dawdawich.client.ByBitWebSocketClient
import space.dawdawich.constants.BYBIT_KLINE_TOPIC
import space.dawdawich.exception.InsufficientBalanceException
import space.dawdawich.integration.client.PrivateHttpClient
import space.dawdawich.model.analyzer.KLineRecord
import space.dawdawich.repositories.mongo.PositionRepository
import space.dawdawich.repositories.mongo.entity.PositionDocument
import space.dawdawich.service.factory.EventListenerFactoryService
import space.dawdawich.strategy.model.KLine
import space.dawdawich.strategy.model.Order
import space.dawdawich.strategy.model.Position
import space.dawdawich.strategy.model.Trend
import space.dawdawich.strategy.strategies.RSIOutBoundStrategyRunner
import space.dawdawich.utils.plusPercent
import space.dawdawich.utils.trimToStep
import java.util.*
import kotlin.time.Duration.Companion.seconds

class CustomRSIOutOfBoundManager(
    private val bybitService: PrivateHttpClient,
    private val webSocket: ByBitWebSocketClient,
    private val eventListenerFactory: EventListenerFactoryService,
    private val positionRepository: PositionRepository,
    symbols: List<String>,
    private val minQtySteps: Map<String, Double>,
    private val maxLeverages: Map<String, Double>,
    private val multiplier: Double,
) {
    private val logger = KotlinLogging.logger {}
    private var priceListener: MessageListenerContainer? = null
    private var kLineListener: MessageListenerContainer? = null
    private val strategyRunner: RSIOutBoundStrategyRunner
    private val setedLeverages: MutableMap<String, Double> =
        mutableMapOf(*maxLeverages.keys.map { it to 1.0 }.toTypedArray())
    private val activateActionMode: MutableMap<String, ActionModel> = mutableMapOf()

    init {
        strategyRunner = RSIOutBoundStrategyRunner(
            runBlocking { bybitService.getAccountBalance() },
            multiplier,
            false,
            5,
            symbols,
            80.0,
            40.0,
            20.0,
            60.0,
            1,
            UUID.randomUUID().toString()
        ) {
                inPrice: Double,
                orderSymbol: String,
                qty: Double,
                refreshTokenUpperBorder: Double,
                refreshTokenLowerBorder: Double,
                trend: Trend,
            ->
            val orderId = UUID.randomUUID().toString()
            val orderQty = qty.trimToStep(minQtySteps[orderSymbol]!!)
            setLeverage(orderSymbol, multiplier)

            val isSuccess =
                runBlocking {
                    try {
                        bybitService.createOrder(
                            orderSymbol,
                            inPrice,
                            orderQty,
                            trend.directionBoolean,
                            orderId,
                            repeatCount = 3,
                            isLimitOrder = false
                        )
                    } catch (e: InsufficientBalanceException) {
                        logger.info { e.message }
                        false
                    }
                }
            if (isSuccess) {
                Order(inPrice, orderSymbol, qty, refreshTokenUpperBorder, refreshTokenLowerBorder, trend, id = orderId)
            } else {
                null
            }
        }.apply {
            setClosePositionFunction { symbol ->
                closeOrdersAndPosition(symbol)
            }

            with(webSocket) {
                customUpdatePositionCallback = { list ->
                    list.forEach { (symbol, position) ->
                        if (position == null) {
                            getPosition(symbol)?.let { positionToClose ->
                                positionRepository.insert(
                                    PositionDocument(
                                        positionToClose.entryPrice,
                                        getCurrentPrice(symbol)!!,
                                        positionToClose.size,
                                        symbol,
                                        positionToClose.createTime,
                                        System.currentTimeMillis(),
                                        positionToClose.trend.name
                                    )
                                )
                            }
                            additionalStrategyAction(symbol)
                        }
                        this@apply.updatePosition(symbol, position)
                    }
                    updateMoney(runBlocking { bybitService.getAccountBalance() })
                }
            }

            priceListener =
                eventListenerFactory.getPriceListenerWithRoutingKey(
                    "tickerWithRSI",
                    "#",
                    object : TypeReference<String>() {}) { symbol, data ->
                    val splitedData = data.split("&")
                    acceptPriceChange(symbol, splitedData[0].toDouble(), splitedData[1].toDouble())
                    if (activateActionMode[symbol] != null) {
                        if (getPosition(symbol) == null) {
                            acceptAction(symbol, splitedData)
                        } else {
                            val moneyWithProfit = money + getPosition(symbol)!!.calculateProfit(splitedData[0].toDouble())
                            if (moneyWithProfit > money.plusPercent(12)) {
                                logger.info { "Closed by TP in action mode" }
                                deactivateAction(symbol)
                            } else if (moneyWithProfit <= money.plusPercent(-6)) {
                                logger.info { "Closed by SL in action mode" }
                                deactivateAction(symbol)
                            }
                        }
                    }
                }.apply { start() }
            kLineListener = eventListenerFactory.getPriceListenerWithRoutingKey(
                BYBIT_KLINE_TOPIC,
                "5.*",
                object : TypeReference<KLineRecord>() {}) { symbol, candle ->
                if (activateActionMode[symbol] == null) {
                    acceptKLine(
                        symbol.split(".")[1],
                        KLine(candle.open, candle.close, candle.high, candle.low, candle.rsi)
                    )
                }
            }.apply { start() }
        }
    }

    private fun deactivateAction(symbol: String) {
        closeOrdersAndPosition(symbol)
        activateActionMode.remove(symbol)
        setLeverage(symbol, multiplier)
    }

    private fun acceptAction(symbol: String, splitedData: List<String>) {
        setLeverage(symbol, 15.0)
        runBlocking {
            try {
                bybitService.createOrder(
                    symbol,
                    splitedData[0].toDouble(),
                    activateActionMode[symbol]!!.qty / multiplier,
                    if (activateActionMode[symbol]!!.trend == Trend.LONG) Trend.SHORT.directionBoolean else Trend.LONG.directionBoolean,
                    UUID.randomUUID().toString(),
                    repeatCount = 3,
                    isLimitOrder = false
                )
            } catch (e: InsufficientBalanceException) {
                logger.info { e.message }
                false
            }
        }
    }

    private fun setLeverage(orderSymbol: String, multiplier: Double) {
        if (setedLeverages[orderSymbol] != multiplier && setedLeverages[orderSymbol] != maxLeverages[orderSymbol]) {
            if (maxLeverages[orderSymbol]!! >= multiplier) {
                runBlocking {
                    bybitService.setMarginMultiplier(orderSymbol, multiplier, retryCount = 3)
                    setedLeverages[orderSymbol] = multiplier
                }
            } else {
                runBlocking {
                    bybitService.setMarginMultiplier(orderSymbol, maxLeverages[orderSymbol]!!, retryCount = 3)
                    setedLeverages[orderSymbol] = maxLeverages[orderSymbol]!!
                }
            }
        }
    }

    private fun additionalStrategyAction(symbol: String) {
        val positionDocuments = positionRepository.findBySymbolOrderByCloseTimeDesc(symbol)
        val positions = positionDocuments.take(2)
            .map { Position(it.entryPrice, it.qty, Trend.valueOf(it.trend)).calculateProfit(it.closePrice) }
        if (positionDocuments.size > 1 && positions
                .all { it < 0 } && positionDocuments[0].trend == positionDocuments[1].trend
        ) {
            activateActionMode[symbol] = ActionModel(Trend.valueOf(positionDocuments[0].trend), positionDocuments[0].qty)
        }
    }

    private fun closeOrdersAndPosition(symbol: String) {
        strategyRunner.getPosition(symbol)?.let { position ->
            do {
                runBlocking {
                    bybitService.closePosition(
                        symbol,
                        position.trend.directionBoolean,
                        position.size,
                        repeatCount = 10
                    )
                    delay(1.seconds)
                }
            } while (strategyRunner.getPosition(symbol) != null)
        }
    }

    data class ActionModel(val trend: Trend, val qty: Double)
}
