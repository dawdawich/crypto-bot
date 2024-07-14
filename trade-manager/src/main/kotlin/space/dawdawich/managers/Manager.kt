package space.dawdawich.managers

import com.fasterxml.jackson.core.type.TypeReference
import kotlinx.coroutines.*
import mu.KLogger
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.MessageListenerContainer
import space.dawdawich.client.ByBitWebSocketClient
import space.dawdawich.constants.*
import space.dawdawich.exception.ApiTokenExpiredException
import space.dawdawich.exception.InsufficientBalanceException
import space.dawdawich.integration.client.PrivateHttpClient
import space.dawdawich.model.RequestProfitableAnalyzer
import space.dawdawich.model.analyzer.KLineRecord
import space.dawdawich.model.constants.Market
import space.dawdawich.model.strategy.CandleTailStrategyConfigModel
import space.dawdawich.model.strategy.GridStrategyConfigModel
import space.dawdawich.model.strategy.GridTableStrategyRuntimeInfoModel
import space.dawdawich.model.strategy.StrategyConfigModel
import space.dawdawich.repositories.mongo.entity.TradeManagerDocument
import space.dawdawich.service.factory.EventListenerFactoryService
import space.dawdawich.strategy.StrategyRunner
import space.dawdawich.strategy.model.KLine
import space.dawdawich.strategy.model.Trend
import space.dawdawich.strategy.strategies.CandleTailStrategyRunner
import space.dawdawich.strategy.strategies.GridTableStrategyRunner
import space.dawdawich.utils.Timer
import java.nio.channels.UnresolvedAddressException
import java.util.concurrent.TimeoutException
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.minutes

@OptIn(DelicateCoroutinesApi::class)
class Manager(
    private val tradeManagerData: TradeManagerDocument,
    private val bybitService: PrivateHttpClient,
    private val rabbitTemplate: RabbitTemplate,
    private val webSocket: ByBitWebSocketClient,
    private val eventListenerFactory: EventListenerFactoryService,
    private val market: Market,
    private val demoAccount: Boolean,
) {
    private infix fun logger(action: (KLogger) -> Unit) {
        MDC.put("manager-id", tradeManagerData.id)
        action(_logger)
        MDC.clear()
    }

    private val initJob: Job
    private val synchronizationObject = Any()
    private val _logger = KotlinLogging.logger {}
    private val pauseTimer = Timer()

    var active: Boolean = true
        private set
    private var lastRefreshTime = System.currentTimeMillis()
    private var priceListener: MessageListenerContainer? = null
    private var kLineListener: MessageListenerContainer? = null
    private var strategyRunner: StrategyRunner? = null
    private var previousPositionTrend: Trend? = null
    private lateinit var crashPostAction: (ex: Exception?) -> Unit

    private var money: Double by Delegates.observable(runBlocking { bybitService.getAccountBalance() }) { _, _, newValue ->
        try {
            strategyRunner?.updateMoney(newValue)
        } catch (e: InsufficientBalanceException) {
            logger { it.warn("Insufficient balance to make this operation") }
            deactivate()
        } catch (e: ApiTokenExpiredException) {
            logger { it.warn("Api token was expired") }
            deactivate()
        } catch (e: UnresolvedAddressException) {
            logger { it.warn("Failed to send request to the market", e) }
        }
    }
    private var currentPrice: Double by Delegates.observable(0.0) { _, oldPrice, newPrice ->
        if (active && !pauseTimer.isTimerActive() && strategyRunner != null && oldPrice > 0 && newPrice > 0) {
            if (strategyRunner?.position == null || strategyRunner!!.position!!.calculateProfit(
                    newPrice
                ) > 0.0
            ) {
                refreshStrategyConfig()
            }
            synchronized(synchronizationObject) {
                when(strategyRunner) {
                    is GridTableStrategyRunner -> acceptGridTableStrategyPriceChange(oldPrice, newPrice)
                    is CandleTailStrategyRunner -> (strategyRunner as CandleTailStrategyRunner).acceptPriceChange(oldPrice, newPrice)
                }
            }
        }
    }

    init {
        var strategyConfig: StrategyConfigModel? = null

        initJob = GlobalScope.launch {
            try {
                while (strategyConfig == null) {
                    strategyConfig = getAnalyzerConfig()
                    if (strategyConfig == null) {
                        delay(tradeManagerData.refreshAnalyzerMinutes.minutes)
                        logger { it.info { "No suitable analyzer not found for manager '${tradeManagerData.id}'" } }
                    }
                }

                logger { it.info { "Initialize Manager with analyzer '${strategyConfig!!.id}'" } }
                setupStrategyRunner(strategyConfig!!)
                lastRefreshTime = System.currentTimeMillis()
            } catch (e: Exception) {
                crashPostAction(e)
            }
        }
    }

    fun setupCrashPostAction(action: (ex: Exception?) -> Unit) {
        crashPostAction = action
    }

    fun getId() = tradeManagerData.id

    fun deactivate(onlyStrategy: Boolean = false) {
        try {
            if (initJob.isCompleted) {
                if (!onlyStrategy) {
                    active = false
                }
                if (strategyRunner != null) {
                    closeOrdersAndPosition()
                }
            }
        } finally {
            if (initJob.isActive) {
                logger { it.info { "DEACTIVATION: cancel init job" } }
                initJob.cancel()
            }
            if (webSocket.isOpen && !onlyStrategy) {
                logger { it.info { "DEACTIVATION: closing web socket" } }
                webSocket.close()
            }
            if (priceListener?.isRunning == true) {
                logger { it.info { "DEACTIVATION: stopping message listener" } }
                priceListener?.stop()
                kLineListener?.stop()
            }
            if (onlyStrategy) {
                logger { it.info { "Manager strategy stopped" } }
            } else {
                logger { it.info { "Manager successfully stopped" } }
            }
        }
    }

    private fun getAnalyzerConfig(currentStrategyId: String? = null): StrategyConfigModel? {
        try {
            return rabbitTemplate.convertSendAndReceive(
                REQUEST_PROFITABLE_ANALYZER_STRATEGY_CONFIG_TOPIC, RequestProfitableAnalyzer(
                    tradeManagerData.accountId,
                    tradeManagerData.chooseStrategy,
                    currentStrategyId,
                    money,
                    demoAccount,
                    market
                )
            ) as StrategyConfigModel?
        } catch (ex: TimeoutException) {
            logger { it.debug { "Do not found strategy for manager. Timestamp '${System.currentTimeMillis()}}'" } }
        }
        return null
    }

    private fun refreshStrategyConfig() {
        if ((System.currentTimeMillis() - lastRefreshTime) > tradeManagerData.refreshAnalyzerMinutes.minutes.inWholeMilliseconds) {
            logger { it.info { "Try to find more suitable analyzer" } }
            getAnalyzerConfig(strategyRunner?.getStrategyConfig()?.id)?.let { config ->
                logger { it.info { "Found more suitable analyzer '${config.id}'" } }
                deactivate(true)
                setupStrategyRunner(config)
            }
            lastRefreshTime = System.currentTimeMillis()
        }
    }

    private fun setupStrategyRunner(strategyConfig: StrategyConfigModel) {
        runBlocking { bybitService.setMarginMultiplier(strategyConfig.symbol, strategyConfig.multiplier) }

        strategyRunner = when (strategyConfig) {
            is GridStrategyConfigModel -> GridTableStrategyRunner(
                strategyConfig.symbol,
                strategyConfig.diapason,
                strategyConfig.gridSize,
                strategyConfig.stopLoss,
                strategyConfig.takeProfit,
                strategyConfig.multiplier,
                money,
                strategyConfig.priceMinStep,
                strategyConfig.minQtyStep,
                false,
                createOrderFunction = getCreateOrderFunction(strategyConfig, bybitService),
                cancelOrderFunction = { symbol, orderId ->
                    logger { it.info { "CLOSING Order: $orderId; Symbol: $symbol" } }
                    runBlocking { bybitService.cancelOrder(symbol, orderId) }
                },
                id = strategyConfig.id
            ).apply {
                setDiapasonConfigs(strategyConfig)
                setClosePosition { isStopLoss ->
                    try {
                        logger { it.info { "CLOSE POSITION: Exceed ${if (isStopLoss) "SL" else "TP"};\n'${position}'" } }
                        closeOrdersAndPosition()
                        if (isStopLoss) {
                            pauseTimer.setTimer(5.minutes.inWholeMilliseconds)
                            lastRefreshTime = 0
                            refreshStrategyConfig()
                        } else {
                            refreshStrategyConfig()
                        }
                    } catch (e: Exception) {
                        crashPostAction(e)
                    }
                }

                with(webSocket) {
                    positionUpdateCallback = { position ->
                        previousPositionTrend = strategyRunner?.position?.trend
                        this@apply.updatePosition(position)
                        if (strategyRunner?.position == null || strategyRunner?.position?.trend != previousPositionTrend) {
                            refreshStrategyConfig()
                        }
                        this@Manager.money = runBlocking {
                            bybitService.getAccountBalance()
                        }
                    }
                    fillOrderCallback = { orderId ->
                        this@apply.fillOrder(orderId)
                    }
                    logger { it.info { "Complete initializing websocket" } }
                }
                priceListener?.stop()
                kLineListener?.stop()

                priceListener =
                    eventListenerFactory.getPriceListener<Double>(
                        if (demoAccount) BYBIT_TEST_TICKER_TOPIC else BYBIT_TICKER_TOPIC,
                        symbol, object : TypeReference<Double>() {})
                        { newPrice ->
                            currentPrice = newPrice
                        }.apply {
                        start()
                    }

            }
            is CandleTailStrategyConfigModel -> CandleTailStrategyRunner(
                strategyConfig.money,
                strategyConfig.multiplier,
                strategyConfig.symbol,
                false,
                strategyConfig.kLineDuration,
                strategyConfig.stopLoss,
                strategyConfig.takeProfit,
                strategyConfig.minQtyStep,
                strategyConfig.id,
                { _, _ -> },
                createOrderFunction = getCreateOrderFunction(strategyConfig, bybitService),
                cancelOrderFunction = { symbol, orderId ->
                    logger { it.info { "CLOSING Order: $orderId; Symbol: $symbol" } }
                    runBlocking { bybitService.cancelOrder(symbol, orderId) }
                },
            ).apply {
                setClosePosition { isStopLoss ->
                    try {
                        logger { it.info { "CLOSE POSITION: Exceed ${if (isStopLoss) "SL" else "TP"};\n'${position}'" } }
                        closeOrdersAndPosition()
                        if (isStopLoss) {
                            lastRefreshTime = 0
                            refreshStrategyConfig()
                        } else {
                            refreshStrategyConfig()
                        }
                    } catch (e: Exception) {
                        crashPostAction(e)
                    }
                }
                priceListener?.stop()
                kLineListener?.stop()

                priceListener =
                    eventListenerFactory.getPriceListener(if (demoAccount) BYBIT_TEST_TICKER_TOPIC else BYBIT_TICKER_TOPIC, symbol, object : TypeReference<Double>() {}) { newPrice ->
                        currentPrice = newPrice
                    }.apply {
                        start()
                    }
                kLineListener = eventListenerFactory.getPriceListener(if (demoAccount) BYBIT_TEST_KLINE_TOPIC else BYBIT_KLINE_TOPIC, "${getStrategyConfig().kLineDuration}.$symbol", object : TypeReference<KLineRecord>() {}) { kLine ->
                    acceptKLine(KLine(kLine.open, kLine.close, kLine.high, kLine.low))
                }.apply { start() }
            }
        }

    }

    private fun acceptGridTableStrategyPriceChange(previousPrice: Double, newPrice: Double) {
        val strategyRunner = this.strategyRunner as GridTableStrategyRunner

        if (strategyRunner.isPriceInBounds(newPrice)) {
            try {
                strategyRunner.acceptPriceChange(previousPrice, newPrice)
            } catch (ex: Exception) {
                logger { it.warn(ex) { "Error occurred." } }
                deactivate()
                crashPostAction.invoke(ex)
            }
        } else {
            strategyRunner.checkStrategyPosition(newPrice)
            logger {
                val (minPrice, maxPrice) = strategyRunner.getPriceBounds()
                it.info { "Price not in price bound. Min Price: '$minPrice'; Max Price: '$maxPrice'; Current: '$newPrice'" }
            }
            rabbitTemplate.convertSendAndReceive(REQUEST_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC, strategyRunner.id)
                ?.let { strategyRuntimeInfo ->
                    when (strategyRuntimeInfo) {
                        is GridTableStrategyRuntimeInfoModel -> {
                            if (strategyRuntimeInfo.middlePrice != strategyRunner.middlePrice) {
                                logger { it.info { "Reinitializing strategy bound" } }
                                closeOrdersAndPosition()
                                strategyRunner.setDiapasonConfigs(strategyRuntimeInfo)
                            }
                        }
                    }
                }
        }
    }

    private fun closeOrdersAndPosition() {
        strategyRunner?.let { strategy ->
            logger { it.info { "Cancel all orders and position" } }
            runBlocking {
                bybitService.cancelAllOrder(strategy.symbol, 10)
                do {
                    strategy.position?.let { position ->
                        bybitService.closePosition(
                            strategy.symbol,
                            position.trend.directionBoolean,
                            position.size,
                            repeatCount = 10
                        )
                    }
                } while (!isPositionEmpty())
                strategy.position = null
            }
        }
    }

    private fun isPositionEmpty() = strategyRunner?.let { strategy ->
        runBlocking { bybitService.getPositionInfo(strategy.symbol).sumOf { position -> position.size } == 0.0 }
    } ?: true
}
