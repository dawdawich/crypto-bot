package space.dawdawich.managers

import kotlinx.coroutines.*
import mu.KLogger
import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.MDC
import org.springframework.kafka.listener.AcknowledgingMessageListener
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate
import space.dawdawich.client.ByBitWebSocketClient
import space.dawdawich.constants.REQUEST_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC
import space.dawdawich.constants.REQUEST_PROFITABLE_ANALYZER_STRATEGY_CONFIG_TOPIC
import space.dawdawich.exception.InvalidSignatureException
import space.dawdawich.integration.client.bybit.ByBitPrivateHttpClient
import space.dawdawich.model.RequestProfitableAnalyzer
import space.dawdawich.model.strategy.GridStrategyConfigModel
import space.dawdawich.model.strategy.GridTableStrategyRuntimeInfoModel
import space.dawdawich.model.strategy.StrategyConfigModel
import space.dawdawich.model.strategy.StrategyRuntimeInfoModel
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.service.factory.PriceTickerListenerFactoryService
import space.dawdawich.strategy.StrategyRunner
import space.dawdawich.strategy.model.CreateOrderFunction
import space.dawdawich.strategy.model.Order
import space.dawdawich.strategy.model.Trend
import space.dawdawich.strategy.strategies.GridTableStrategyRunner
import space.dawdawich.utils.trimToStep
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(DelicateCoroutinesApi::class)
class Manager(
    private val tradeManagerData: TradeManagerDocument,
    private val bybitService: ByBitPrivateHttpClient,
    private val replayingStrategyConfigKafkaTemplate: ReplyingKafkaTemplate<String, RequestProfitableAnalyzer, StrategyConfigModel?>,
    private val replayingStrategyDataKafkaTemplate: ReplyingKafkaTemplate<String, String, StrategyRuntimeInfoModel?>,
    private val webSocket: ByBitWebSocketClient,
    private val priceListenerFactory: PriceTickerListenerFactoryService,
) {

    private val initJob: Job
    private val synchronizationObject = Any()
    private val _logger = KotlinLogging.logger {}

    private var money: Double by Delegates.observable(runBlocking {bybitService.getAccountBalance() }) { _, _, newValue ->
        strategyRunner.updateMoney(newValue)
    }

    private infix fun logger(action: (KLogger) -> Unit) {
        MDC.put("manager-id", tradeManagerData.id)
        action(_logger)
        MDC.clear()
    }

    private var currentPrice: Double by Delegates.observable(0.0) { _, oldPrice, newPrice ->
        if (oldPrice > 0 && newPrice > 0) {
            logger { it.info { "Accept price change. old price: '$oldPrice'; new price: '$newPrice'" } }
            strategyRunner.acceptPriceChange(oldPrice, newPrice)
        }
    }

    private var lastRefreshTime = System.currentTimeMillis()
    private var listener: ConcurrentMessageListenerContainer<String, String>? = null
    private lateinit var strategyRunner: StrategyRunner
    private lateinit var crashPostAction: (ex: Exception?) -> Unit

    var active: Boolean = true
        private set

    init {
        var strategyConfig: StrategyConfigModel? = null

        initJob = GlobalScope.launch {
            while (strategyConfig == null) {
                strategyConfig = getAnalyzerConfig()
                if (strategyConfig == null) {
                    delay(tradeManagerData.refreshAnalyzerMinutes.minutes)
                }
            }

            logger { it.info { "Initialize Manager with analyzer '${strategyConfig!!.id}'" } }
            setupStrategyRunner(strategyConfig!!)
            lastRefreshTime = System.currentTimeMillis()
        }
    }

    fun setupCrashPostAction(action: (ex: Exception?) -> Unit) {
        crashPostAction = action
    }

    fun deactivate(onlyStrategy: Boolean = false) {
        try {
            if (initJob.isCompleted) {
                synchronized(synchronizationObject) {
                    if (!onlyStrategy) {
                        active = false
                    }
                    strategyRunner.position?.let { position ->
                        runBlocking {
                            bybitService.cancelAllOrder(strategyRunner.symbol)
                            bybitService.closePosition(
                                strategyRunner.symbol,
                                position.trend.direction == 1,
                                position.size
                            )
                            delay(5.seconds)
                        }
                        if (strategyRunner.position != null) {
                            crashPostAction.invoke(null)
                        }
                    }
                }
            }
        } finally {
            if (initJob.isActive) {
                initJob.cancel()
            }
            if (webSocket.isOpen && !onlyStrategy) {
                webSocket.close()
            }
            if (listener?.isRunning == true) {
                listener?.stop()
            }
            logger { it.info { "Manager successfully stopped" } }
        }
    }

    fun getId() = tradeManagerData.id

    private fun getAnalyzerConfig(strategyConfigModel: StrategyConfigModel? = null): StrategyConfigModel? {
        try {
            return replayingStrategyConfigKafkaTemplate.sendAndReceive(
                ProducerRecord(
                    REQUEST_PROFITABLE_ANALYZER_STRATEGY_CONFIG_TOPIC,
                    RequestProfitableAnalyzer(
                        tradeManagerData.accountId,
                        tradeManagerData.chooseStrategy,
                        strategyConfigModel?.id,
                        money
                    )
                )
            ).get(5, TimeUnit.SECONDS).value()
        } catch (ex: TimeoutException) {
            logger { it.debug { "Do not found strategy for manager. Timestamp '${System.currentTimeMillis()}}'" } }
        }
        return null
    }

    private fun refreshStrategyConfig() {
        if ((System.currentTimeMillis() - lastRefreshTime) > tradeManagerData.refreshAnalyzerMinutes.minutes.inWholeMilliseconds) {
            logger { it.info { "Try to find more suitable analyzer" } }
            getAnalyzerConfig(strategyRunner.getStrategyConfig())?.let { config ->
                logger { it.info { "Found more suitable analyzer '${config.id}'" } }
                deactivate(true)
                setupStrategyRunner(config)
            }
            lastRefreshTime = System.currentTimeMillis()
        }
    }

    private fun setupStrategyRunner(strategyConfig: StrategyConfigModel) {
        runBlocking { bybitService.setMarginMultiplier(strategyConfig.symbol, strategyConfig.multiplier) }
        val createOrderFunction: CreateOrderFunction = {
                inPrice: Double,
                orderSymbol: String,
                qty: Double,
                refreshTokenUpperBorder: Double,
                refreshTokenLowerBorder: Double,
                trend: Trend,
            ->
            val orderId = UUID.randomUUID().toString()
            val orderQty = qty.trimToStep(strategyConfig.minQtyStep)
            logger { it.info { "Try to create order. id: '$orderId'; price: '$inPrice'; qty: $orderQty; symbol: $orderSymbol; minOrderQty: ${strategyConfig.minQtyStep}; priceTick: ${strategyConfig.priceMinStep}" } }
            val isSuccess =
                runBlocking {
                    bybitService.createOrder(
                        orderSymbol,
                        inPrice,
                        orderQty,
                        trend.directionBoolean,
                        orderId
                    )
                }
            if (isSuccess) {
                logger { it.info { "Order '$orderId' created successfully" } }
                Order(inPrice, orderSymbol, qty, refreshTokenUpperBorder, refreshTokenLowerBorder, trend, id = orderId)
            } else {
                logger { it.info { "Order '$orderId' do not created" } }
                null
            }
        }

        strategyRunner = when (strategyConfig) {
            is GridStrategyConfigModel -> {
                GridTableStrategyRunner(
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
                    createOrderFunction = createOrderFunction,
                    cancelOrderFunction = { symbol, orderId ->
                        logger { it.info { "CLOSING Order: $orderId; Symbol: $symbol" } }
                        runBlocking {
                            bybitService.cancelOrder(symbol, orderId)
                        }
                    },
                    id = strategyConfig.id
                ).apply {
                    setDiapasonConfigs(
                        strategyConfig.middlePrice,
                        strategyConfig.minPrice,
                        strategyConfig.maxPrice,
                        strategyConfig.step,
                        strategyConfig.pricesGrid.map { it.trimToStep(strategyConfig.priceMinStep) }.toSet()
                    )
                    setClosePosition {
                        logger { it.info { "CLOSE POSITION: Get TP/SL;\n'${position}'" } }
                        position?.let { pos ->
                            runBlocking { bybitService.closePosition(symbol, pos.trend.directionBoolean, pos.size) }
                        }
                        webSocket.resetCumRealizedPnL()
                    }
                    with(webSocket) {
                        positionUpdateCallback = { position ->
                            this@apply.updatePosition(position)
                            this@Manager.money = runBlocking {
                                bybitService.getAccountBalance()
                            }
                        }
                        fillOrderCallback = { orderId ->
                            this@apply.fillOrder(orderId)
                        }
                        logger { it.info { "Complete initializing websocket" } }
                        if (!isOpen) {
                            logger { it.info { "Connecting to web socket" } }
                            connectBlocking()
                        }
                    }
                    listener = priceListenerFactory.getPriceListener(symbol, true).apply {
                        setupMessageListener(getMessageListener(strategyConfig))
                        start()
                    }
                }
            }
        }
    }

    private fun GridTableStrategyRunner.getMessageListener(strategyConfig: GridStrategyConfigModel): AcknowledgingMessageListener<String, String> =
        AcknowledgingMessageListener { message, acknowledgment ->
            acknowledgment?.acknowledge()
            try {
                val price = message.value().toDouble()
                if (!isPriceInBounds(price)) {
                    logger {
                        val (minPrice, maxPrice) = getPriceBounds()
                        it.info { "Price not in price bound. Min Price: '$minPrice'; Max Price: '$maxPrice'; Current: '$price'" }
                    }
                    val runtimeData = replayingStrategyDataKafkaTemplate.sendAndReceive(
                        ProducerRecord(
                            REQUEST_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC, strategyConfig.id
                        )
                    ).get(5, TimeUnit.SECONDS).value()

                    runtimeData?.let { data ->
                        if (data is GridTableStrategyRuntimeInfoModel && this.middlePrice != data.middlePrice) {
                            logger { it.info { "Start to reinitialize strategy bounds" } }
                            runBlocking {
                                bybitService.cancelAllOrder(strategyConfig.symbol)
                                position?.let { pos ->
                                    logger { it.info { "CLOSE POSITION: change middle price" } }
                                    bybitService.closePosition(
                                        strategyConfig.symbol,
                                        pos.trend.directionBoolean,
                                        pos.size
                                    )
                                    webSocket.resetCumRealizedPnL()
                                }
                            }
                            this.setDiapasonConfigs(
                                data.middlePrice,
                                data.minPrice,
                                data.maxPrice,
                                data.step,
                                data.prices.map { it.trimToStep(strategyConfig.priceMinStep) }.toSet()
                            )
                        }
                    }
                } else { // Processed only with active web socket connection
                    synchronized(synchronizationObject) {
                        if (active) {
                            try {
                                currentPrice = price
                            } catch (ex: InvalidSignatureException) {
                                logger { it.warn { "Signature become invalid" } }
                                webSocket.reconnectBlocking()
                            }
                        }
                    }
                }
                refreshStrategyConfig()
            } catch (ex: Exception) {
                logger { it.warn(ex) { "Error occurred." } }
                deactivate()
                crashPostAction.invoke(ex)
            }
        }
}
