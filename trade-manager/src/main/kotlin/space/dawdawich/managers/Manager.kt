package space.dawdawich.managers

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KLogger
import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.MDC
import org.springframework.kafka.listener.AcknowledgingMessageListener
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate
import space.dawdawich.client.ByBitWebSocketClient
import space.dawdawich.constants.REQUEST_ANALYZER_STRATEGY_CONFIG_TOPIC
import space.dawdawich.constants.REQUEST_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC
import space.dawdawich.integration.client.bybit.ByBitPrivateHttpClient
import space.dawdawich.model.strategy.configModel.GridStrategyConfigModel
import space.dawdawich.model.strategy.runtimeModel.GridTableStrategyRuntimeInfoModel
import space.dawdawich.model.strategy.configModel.StrategyConfigModel
import space.dawdawich.model.strategy.configModel.SwitchStrategyConfigModel
import space.dawdawich.model.strategy.runtimeModel.StrategyRuntimeInfoModel
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.service.factory.PriceTickerListenerFactoryService
import space.dawdawich.strategy.StrategyRunner
import space.dawdawich.strategy.model.*
import space.dawdawich.strategy.strategies.GridTableStrategyRunner
import space.dawdawich.strategy.strategies.SwitchStrategyRunner
import space.dawdawich.utils.trimToStep
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.NoSuchElementException
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.seconds

class Manager(
    private val tradeManagerData: TradeManagerDocument,
    private val bybitService: ByBitPrivateHttpClient,
    replayingStrategyConfigKafkaTemplate: ReplyingKafkaTemplate<String, String, StrategyConfigModel?>,
    private val replayingStrategyDataKafkaTemplate: ReplyingKafkaTemplate<String, String, StrategyRuntimeInfoModel?>,
    private val webSocket: ByBitWebSocketClient,
    priceListenerFactory: PriceTickerListenerFactoryService
) {

    private val synchronizationObject = Any()
    private val _logger = KotlinLogging.logger {}
    private infix fun logger(action: (KLogger) -> Unit) {
        MDC.put("manager-id", tradeManagerData.id)
        action(_logger)
        MDC.clear()
    }

    private var currentPrice: Double by Delegates.observable(0.0) { _, oldPrice, newPrice ->
        if (oldPrice > 0 && newPrice > 0) {
            strategyRunner.acceptPriceChange(oldPrice, newPrice)
        }
    }

    private var listener: ConcurrentMessageListenerContainer<String, String>? = null
    private var active: Boolean = true
    private var strategyRunner: StrategyRunner

    private lateinit var crashPostAction: (ex: Exception?) -> Unit

    init {
        var strategyConfig: StrategyConfigModel? = null

        while (strategyConfig == null) {
            strategyConfig = replayingStrategyConfigKafkaTemplate.sendAndReceive(
                ProducerRecord(REQUEST_ANALYZER_STRATEGY_CONFIG_TOPIC, tradeManagerData.accountId)
            ).get(5, TimeUnit.SECONDS).value()
        }

        val money = runBlocking { bybitService.getAccountBalance() }
        runBlocking { bybitService.setMarginMultiplier(strategyConfig.symbol, strategyConfig.multiplier) }
        val createGridTableOrderFunction: CreateGridTableOrderFunction = { inPrice: Double,
                                                         orderSymbol: String,
                                                         qty: Double,
                                                         refreshTokenUpperBorder: Double,
                                                         refreshTokenLowerBorder: Double,
                                                         trend: Trend ->
            val orderId = UUID.randomUUID().toString()
            val isSuccess =
                runBlocking {
                    bybitService.createOrder(
                        orderSymbol,
                        inPrice,
                        qty.trimToStep(strategyConfig.minQtyStep),
                        trend.directionBoolean,
                        orderId
                    )
                }
            if (isSuccess) {
                GridTableOrder(inPrice, orderSymbol, qty, refreshTokenUpperBorder, refreshTokenLowerBorder, trend, id = orderId)
            } else {
                null
            }
        }

        val createSwitchOrderFunction: CreateSwitchOrderFunction = { inPrice: Double,
                                                         orderSymbol: String,
                                                         qty: Double,
                                                         trend: Trend ->
            val orderId = UUID.randomUUID().toString()
            val isSuccess =
                runBlocking {
                    bybitService.createOrder(
                        orderSymbol,
                        inPrice,
                        qty.trimToStep(strategyConfig.minQtyStep),
                        trend.directionBoolean,
                        orderId
                    )
                }
            if (isSuccess) {
                Order(inPrice, orderSymbol, qty, trend, id = orderId)
            } else {
                null
            }
        }

        var messageListener: AcknowledgingMessageListener<String, String>

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
                    createGridTableOrderFunction = createGridTableOrderFunction,
                ).apply {
                    setDiapasonConfigs(strategyConfig)
                    setClosePosition {
                        runBlocking { bybitService.closePosition(symbol, it.trend.directionBoolean, it.size) }
                    }
                    webSocket.positionUpdateCallback = { position ->
                        this.updatePosition(position)
                        this.updateMoney(runBlocking {
                            bybitService.getAccountBalance()
                        })
                    }
                    webSocket.fillOrderCallback = {
                        this.fillOrder(it)
                    }
                    webSocket.connect()
                    messageListener = AcknowledgingMessageListener { message, acknowledgment ->
                        acknowledgment?.acknowledge()
                        try {
                            val price = message.value().toDouble()
                            if (!isPriceInBounds(price)) {
                                val runtimeData = replayingStrategyDataKafkaTemplate.sendAndReceive(
                                    ProducerRecord(
                                        REQUEST_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC, strategyConfig.id
                                    )
                                ).get(5, TimeUnit.SECONDS).value()

                                runtimeData?.let { data ->
                                    if (data is GridTableStrategyRuntimeInfoModel && this.middlePrice != data.middlePrice) {
                                        runBlocking {
                                            bybitService.cancelAllOrder(strategyConfig.symbol)
                                            position?.let { pos ->
                                                bybitService.closePosition(
                                                    strategyConfig.symbol,
                                                    pos.trend.directionBoolean,
                                                    pos.size
                                                )
                                            }
                                        }
                                        this.setDiapasonConfigs(data)
                                    }
                                }
                            } else {
                                synchronized(synchronizationObject) {
                                    if (active) {
                                        currentPrice = price
                                    }
                                }
                            }
                        } catch (ex: Exception) {
                            deactivate()
                            crashPostAction.invoke(ex)
                        }
                    }
                }
            }
// TODO add SwitchStrategyRunner
            is SwitchStrategyConfigModel -> {
                SwitchStrategyRunner(
                        strategyConfig.symbol,
                        strategyConfig.multiplier,
                        strategyConfig.money,
                        false,
                        strategyConfig.priceMinStep,
                        strategyConfig.minQtyStep,
                        strategyConfig.capitalOrderPerPercent,
                        strategyConfig.switchCounterValue,
                        strategyConfig.coefficientBetweenOrders,
                        createSwitchOrderFunction = createSwitchOrderFunction
                ) .apply {

                }
            }

            else -> throw NoSuchElementException("No such strategy config")
        }
        listener = priceListenerFactory.getPriceListener(strategyRunner.symbol, true).apply {
            setupMessageListener(messageListener)
            start()
        }
    }

    fun setupCrashPostAction(action: (ex: Exception?) -> Unit) {
        crashPostAction = action
    }

    fun deactivate() {
        try {
            synchronized(synchronizationObject) {
                active = false
                strategyRunner.position?.let { position ->
                    runBlocking {
                        launch {
                            bybitService.closePosition(
                                strategyRunner.symbol,
                                position.trend.direction == 1,
                                position.size
                            )
                        }
                        launch {
                            bybitService.cancelAllOrder(strategyRunner.symbol)
                        }
                        delay(5.seconds)
                    }
                    if (strategyRunner.position != null) {
                        crashPostAction.invoke(null)
                    }
                }
            }
        } finally {
            if (webSocket.isOpen) {
                webSocket.close()
            }
            if (listener?.isRunning == true) {
                listener?.stop()
            }
        }
    }

    fun getId() = tradeManagerData.id
}
