package space.dawdawich.strategy.strategies

import space.dawdawich.model.strategy.GridStrategyConfigModel
import space.dawdawich.model.strategy.GridTableStrategyRuntimeInfoModel
import space.dawdawich.strategy.StrategyRunner
import space.dawdawich.strategy.model.*
import space.dawdawich.utils.plusPercent
import java.util.*
import kotlin.math.absoluteValue
import kotlin.properties.Delegates

class GridTableStrategyRunner(
    symbol: String,
    private val diapason: Int,
    private val gridSize: Int,
    private val stopLoss: Int,
    private val takeProfit: Int,
    multiplier: Int,
    money: Double,
    priceMinStep: Double,
    minQtyStep: Double,
    simulateTradeOperations: Boolean,
    moneyChangePostProcessFunction: MoneyChangePostProcessFunction = { _, _ -> },
    updateMiddlePrice: UpdateMiddlePricePostProcessFunction = { _ -> },
    private val createOrderFunction: CreateOrderFunction = { inPrice: Double, orderSymbol: String, qty: Double, refreshTokenUpperBorder: Double, refreshTokenLowerBorder: Double, trend: Trend ->
        Order(inPrice, orderSymbol, qty, refreshTokenUpperBorder, refreshTokenLowerBorder, trend)
    },
    id: String = UUID.randomUUID().toString(),
) : StrategyRunner(
    money,
    multiplier,
    moneyChangePostProcessFunction,
    priceMinStep,
    minQtyStep,
    symbol,
    simulateTradeOperations,
    id
) {
    private val synchronizeObject: Any = Any()
    private val orderPriceGrid: MutableMap<Double, Order?> = mutableMapOf()
    private var minPrice: Double = -1.0
    private var maxPrice: Double = -1.0
    private var step: Double = 0.0
    private var priceOutOfDiapasonCounter = 0
    var middlePrice: Double by Delegates.observable(0.0) { _, _, newValue ->
        updateMiddlePrice(newValue)
    }
        private set

    fun fillOrder(orderId: String) {
        synchronized(synchronizeObject) {
            if (!simulateTradeOperations) {
                orderPriceGrid.values.filterNotNull().find { order -> order.id == orderId }?.isFilled = true
            }
        }
    }

    fun setDiapasonConfigs(
        middlePrice: Double,
        minPrice: Double,
        maxPrice: Double,
        step: Double,
        orderPrices: Set<Double>,
    ) {
        this.middlePrice = middlePrice
        this.minPrice = minPrice
        this.maxPrice = maxPrice
        this.step = step
        this.orderPriceGrid += orderPrices.map { it to null }
    }

    fun isPriceInBounds(price: Double) = price in minPrice..maxPrice

    fun getPriceBounds() = minPrice to maxPrice

    override fun getRuntimeInfo() = GridTableStrategyRuntimeInfoModel(
        id,
        orderPriceGrid.keys,
        currentPrice,
        middlePrice,
        minPrice,
        maxPrice,
        step,
        position?.convertToInfo()
    )

    override fun getStrategyConfig() =
        GridStrategyConfigModel(
            id,
            symbol,
            money,
            multiplier,
            diapason,
            gridSize,
            stopLoss,
            takeProfit,
            priceMinStep,
            minQtyStep,
            middlePrice,
            minPrice,
            maxPrice,
            step,
            orderPriceGrid.keys
        )

    override fun acceptPriceChange(previousPrise: Double, currentPrice: Double) {
        synchronized(synchronizeObject) {
            this.currentPrice = currentPrice

            if (simulateTradeOperations) {
                if (minPrice <= 0.0 && maxPrice <= 0.0) {
                    setUpPrices(currentPrice)
                }

                checkPriceForSetupBounds(currentPrice)
            }

            processOrders(currentPrice, previousPrise)

            position?.let { position ->
                if (position.size > 0.0) {
                    moneyWithProfit =
                        money + position.calculateProfit(currentPrice) // Process data to update in db including calculation profits/loses
                    if (moneyWithProfit > money.plusPercent(takeProfit) || moneyWithProfit < money.plusPercent(-stopLoss)) {
                        money = moneyWithProfit
                        closePositionFunction.invoke()
                    }
                }
            }
        }
    }

    private fun setUpPrices(currentPrice: Double) {
        val step = (currentPrice.plusPercent(diapason) - currentPrice.plusPercent(-diapason)) / gridSize

        var minPrice = currentPrice
        var maxPrice = currentPrice
        val gridPrices = mutableListOf<Double>()

        minPrice -= step
        maxPrice += step
        repeat(gridSize / 2) {
            minPrice -= step
            maxPrice += step
            gridPrices += minPrice
            gridPrices += maxPrice
        }

        this.minPrice = minPrice
        this.maxPrice = maxPrice

        orderPriceGrid.clear()
        orderPriceGrid += gridPrices.map { it to null }
        middlePrice = currentPrice
        this.step = step
    }

    private fun checkPriceForSetupBounds(currentPrice: Double) {
        if (currentPrice !in minPrice..maxPrice && priceOutOfDiapasonCounter++ > 30) {
            money += position?.calculateProfit(currentPrice) ?: 0.0
            position = null

            setUpPrices(currentPrice)
            priceOutOfDiapasonCounter = 0
        } else if (currentPrice in minPrice..maxPrice) {
            priceOutOfDiapasonCounter = 0
        }
    }

    private fun processOrders(currentPrice: Double, previousPrice: Double) {
        val moneyPerOrder = money / gridSize

        if ((position?.getPositionValue() ?: 0.0) / multiplier + step < money) {
            orderPriceGrid.entries
                .asSequence()
                .filter { (it.key - currentPrice).absoluteValue > priceMinStep }
                .sortedBy { (it.key - currentPrice).absoluteValue }
                .take(2)
                .filter { it.value == null }
                .map { it.key }
                .forEach { inPrice ->
                    val orderTrend = if (inPrice < middlePrice) Trend.LONG else Trend.SHORT
                    val qty = moneyPerOrder * multiplier / inPrice

                    if (position?.trend != orderTrend && (position?.calculateReduceOrder(inPrice, qty, orderTrend) ?: 0.0) < 0) {
                        return@forEach
                    }

                    if (((position?.getPositionValue() ?: 0.0) + (inPrice * qty)) / multiplier > money) {
                        return@forEach
                    }

                    val order = createOrderFunction(
                        inPrice,
                        symbol,
                        qty,
                        inPrice - step * orderTrend.direction,
                        inPrice + step * orderTrend.direction,
                        orderTrend
                    )

                    orderPriceGrid[inPrice] = order
                }
        }

        orderPriceGrid.values.filterNotNull().forEach { order ->
            if (!order.isFilled) {
                if (simulateTradeOperations) {
                    order.isFilled =
                        (order.inPrice > previousPrice && order.inPrice <= currentPrice) ||
                                (order.inPrice < previousPrice && order.inPrice >= currentPrice)
                    if (order.isFilled) {
                        position?.updateSizeAndEntryPrice(order) ?: run {
                            position = Position(order.inPrice, order.count, order.trend)
                        }
                        position?.let { if (it.size <= 0) position = null }
                    }
                }
            } else if (order.isPriceOutOfRefreshBorder(currentPrice)) {
                order.isFilled = false
                orderPriceGrid[orderPriceGrid.entries.first { it.value == order }.key] = null
            }
        }
    }
}
