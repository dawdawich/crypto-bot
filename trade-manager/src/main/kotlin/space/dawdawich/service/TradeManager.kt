package space.dawdawich.service

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import kotlinx.coroutines.runBlocking
import netscape.javascript.JSObject
import org.json.JSONObject
import org.springframework.data.domain.Pageable
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import space.dawdawich.client.ByBitWebSocketClient
import space.dawdawich.repositories.GridTableAnalyzerRepository
import space.dawdawich.repositories.entity.AnalyzerChooseStrategy
import space.dawdawich.repositories.entity.GridTableAnalyzerDocument
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.service.model.Order
import space.dawdawich.service.model.Position
import space.dawdawich.utils.bytesToHex
import space.dawdawich.utils.plusPercent
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.absoluteValue

class TradeManager(
    private val tradeManagerData: TradeManagerDocument,
    private val priceTickerListenerFactoryService: PriceTickerListenerFactoryService,
    private val analyzerRepository: GridTableAnalyzerRepository,
    private val orderService: ByBitOrderHttpService,
    private val apiKey: String,
    secretKey: String
) {
    private var priceListener: ConcurrentMessageListenerContainer<String, String>? = null
    private var analyzer: GridTableAnalyzerDocument? = null

    private val encryptor: Mac = Mac.getInstance("HmacSHA256")
    private val webSocketClient: ByBitWebSocketClient
    private val jsonPath =
        JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))

    private var capital = 0.0
    private var priceInstruction: Pair<Double, Double> = 0.0 to 0.0

    private var price: Double = -1.0
    private var middlePrice: Double = -1.0
    private var orderPriceGrid: MutableMap<Double, Order?> = mutableMapOf()
    private var longPosition: Position? = null
    private var shortPosition: Position? = null

    init {
        encryptor.init(SecretKeySpec(secretKey.toByteArray(), "HmacSHA256"))
        setupAnalyzer()
        updateCapital()

        webSocketClient = ByBitWebSocketClient(
            apiKey,
            encryptor,
            { positionResponse ->
                updatePosition(
                    Position(
                        positionResponse.symbol,
                        positionResponse.side.equals("buy", true),
                        positionResponse.size.toDouble(),
                        positionResponse.entryPrice.toDouble(),
                        positionResponse.updatedTime.toLong()
                    )
                )
            },
            { orderResponse ->
                updateOrder(
                    Order(
                        orderResponse.symbol,
                        orderResponse.side.equals("buy", true),
                        orderResponse.price.toDouble(),
                        orderResponse.qty.toDouble(),
                        orderResponse.orderStatus,
                        orderResponse.orderLinkId
                    )
                )
            }
        )
    }

    private fun updateCapital() {
        val timestamp = System.currentTimeMillis()
        val balanceResponse = runBlocking {
            orderService.getAccountBalance(apiKey, timestamp) { body ->
                getSign(
                    body,
                    timestamp.toString()
                )
            }
        }

        capital =
            jsonPath.parse(balanceResponse).read<String?>("\$.result.list[0].coin[0].walletBalance")?.toDouble() ?: 0.0
        priceInstruction = runBlocking { orderService.getPairInstructions(analyzer!!.symbolInfo.symbol) }
    }

    private fun updatePosition(position: Position) {
        println("Position Update: $position")
        if (longPosition != null && position.isLong && position.updateTime > longPosition!!.updateTime) {
            longPosition = position
        }
        if (shortPosition != null && !position.isLong && position.updateTime > shortPosition!!.updateTime) {
            shortPosition = position
        }
    }

    private fun updateOrder(order: Order) {
        println("Obtained order to update; id: ${order.orderLinkId}, status: ${order.orderStatus}")
        val priceKey = orderPriceGrid.entries.first { it.value?.orderLinkId == order.orderLinkId }.key
        orderPriceGrid.remove(priceKey)
        orderPriceGrid[priceKey] = order
    }

    private fun updatePrice(newPrice: Double) {
        if (tradeManagerData.isActive && analyzer != null) {
            if (price < 0) {
                middlePrice = newPrice

                val minPrice = middlePrice.plusPercent(-analyzer!!.diapason)
                val maxPrice = middlePrice.plusPercent(analyzer!!.diapason)
                val step = (maxPrice - minPrice) / analyzer!!.gridSize

                val gridPrices = mutableListOf<Double>()
                repeat(analyzer!!.gridSize) {
                    gridPrices += minPrice + step * it
                }
                orderPriceGrid = mutableMapOf(*gridPrices.map { it to null }.toTypedArray())
            }

            price = newPrice

            checkOrders()
        }
    }

    private fun checkOrders() {
        val nearOrders = orderPriceGrid.entries.sortedBy { (it.key - price).absoluteValue }.take(2)

        nearOrders.forEach {
            if (it.value == null) {
                val moneyPerPosition = capital / analyzer!!.gridSize

                val inPrice = BigDecimal(it.key).setScale(
                    priceInstruction.second.toString().split(".")[1].length,
                    RoundingMode.HALF_DOWN
                ).toDouble()

                val qty = BigDecimal(moneyPerPosition * analyzer!!.multiplayer / inPrice).setScale(
                    priceInstruction.first.toString().split(".")[1].length, RoundingMode.DOWN
                ).toDouble()
                val timestamp = System.currentTimeMillis()
                // create order
                val isLong = it.key < middlePrice
                val symbol = analyzer!!.symbolInfo.symbol
                val orderId: String = UUID.randomUUID().toString()
                val response = runBlocking {
                    orderService.createOrder(
                        symbol,
                        it.key,
                        qty,
                        isLong,
                        orderId,
                        apiKey,
                        timestamp
                    ) { body -> getSign(body, timestamp.toString()) }
                }

                response?.let { jsonResponse ->
                    JSONObject(jsonResponse).apply {
                        if (getInt("retCode") == 0) {
                            orderPriceGrid[it.key] = Order(symbol, isLong, it.key, qty, "Requested", orderId)
                            println("Created HTTP order id: $orderId")
                        }
                    }
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
            setPriceListener()
        }
    }

    private fun setPriceListener() {
        priceListener?.stop()
        priceListener = priceTickerListenerFactoryService.getPriceListener(analyzer!!.symbolInfo.symbol)
        priceListener!!.setupMessageListener(MessageListener<String, String> {
            updatePrice(it.value().toDouble())
            println("Update price in manager '${tradeManagerData.id}'; price - $price")
        })
        priceListener!!.start()
    }

    private fun getSign(body: String, timestamp: String): String {
        return encryptor.doFinal("$timestamp${apiKey}5000$body".toByteArray()).bytesToHex()
    }

    fun getId() = tradeManagerData.id
}
