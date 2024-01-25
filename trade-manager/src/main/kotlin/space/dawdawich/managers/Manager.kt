package space.dawdawich.managers

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate
import space.dawdawich.client.ByBitWebSocketClient
import space.dawdawich.constants.REQUEST_ANALYZER_STRATEGY_CONFIG_TOPIC
import space.dawdawich.integration.client.bybit.ByBitPrivateHttpClient
import space.dawdawich.model.strategy.GridStrategyConfigModel
import space.dawdawich.model.strategy.StrategyConfigModel
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.service.PriceTickerListenerFactoryService
import space.dawdawich.strategy.StrategyRunner
import space.dawdawich.strategy.strategies.GridTableStrategyRunner
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.seconds

class Manager<T : StrategyRunner>(
    private val tradeManagerData: TradeManagerDocument,
    private val bybitService: ByBitPrivateHttpClient,
    private val replayingKafkaTemplate: ReplyingKafkaTemplate<String, String, StrategyConfigModel?>,
    private val webSocket: ByBitWebSocketClient,
    private val priceListenerFactory: PriceTickerListenerFactoryService
) {

    private var listener: ConcurrentMessageListenerContainer<String, String>? = null
    private var strategyRunner: T
    private var active: Boolean = true
    private var price = 0.0

    init {
        var strategyConfig: StrategyConfigModel? = null

        while (strategyConfig == null) {
            strategyConfig = replayingKafkaTemplate.sendAndReceive(
                ProducerRecord(REQUEST_ANALYZER_STRATEGY_CONFIG_TOPIC, tradeManagerData.accountId)
            ).get(5, TimeUnit.SECONDS).value()
        }

        val money = runBlocking { bybitService.getAccountBalance() }

        strategyRunner = when (strategyConfig) {
            is GridStrategyConfigModel -> {
                val strategy = GridTableStrategyRunner(
                    strategyConfig.symbol,
                    strategyConfig.diapason,
                    strategyConfig.gridSize,
                    strategyConfig.stopLoss,
                    strategyConfig.takeProfit,
                    strategyConfig.multiplier,
                    money,
                    strategyConfig.priceMinStep,
                    false,
                    strategyConfig.middlePrice,
                    moneyChangePostProcessFunction = { _, _ -> },
                    minPrice = strategyConfig.minPrice,
                    maxPrice = strategyConfig.maxPrice,
                    step = strategyConfig.step
                )
                webSocket.positionUpdateCallback = { position ->
                    strategy.updatePosition(position)
                }
                webSocket.fillOrderCallback = { orderId ->
                    strategy.fillOrder(orderId)
                }
                strategy as T
            }
            else -> throw Exception()
        }
        listener = priceListenerFactory.getPriceListener(strategyRunner.symbol, true).apply {
            setupMessageListener(MessageListener<String, String> {
                try {
                    val newPrice = it.value().toDouble()
                    strategyRunner.acceptPriceChange(price, newPrice)
                    price = newPrice
                } catch (ex: Exception) {
                    deactivate()
                }
            })
            start()
        }
    }

    private var currentPrice: Double by Delegates.observable(0.0) { _, oldPrice, newPrice ->
        if (oldPrice > 0 && newPrice > 0) {
            strategyRunner.acceptPriceChange(oldPrice, newPrice)
        }
    }

    fun deactivate() {
        try {
            active = false
            strategyRunner.position?.let { position ->
                runBlocking {
                    bybitService.closePosition(
                        strategyRunner.symbol,
                        position.trend.direction == 1,
                        position.size
                    )
                    delay(5.seconds)
                }
                if (strategyRunner.position != null) {
                    throw Exception() // Failed to deactivate manager
                }
            }
        } finally {
            webSocket.close()
            if (listener?.isRunning == true) {
                listener?.stop()
            }
        }
    }

    fun acceptPriceChange(currentPrice: Double) {
        this.currentPrice = currentPrice
    }

    fun getRuntimeInfo() = strategyRunner.getRuntimeInfo()

    fun getId() = tradeManagerData.id
}
