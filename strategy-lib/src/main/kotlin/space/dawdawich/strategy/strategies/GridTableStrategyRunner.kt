package space.dawdawich.strategy.strategies

import space.dawdawich.model.strategy.GridStrategyConfigModel
import space.dawdawich.model.strategy.GridTableStrategyRuntimeInfoModel
import space.dawdawich.strategy.model.CreateOrderFunction
import space.dawdawich.strategy.model.MoneyChangePostProcessFunction
import space.dawdawich.strategy.StrategyRunner
import space.dawdawich.strategy.model.UpdateMiddlePricePostProcessFunction
import space.dawdawich.strategy.model.Order
import space.dawdawich.strategy.model.Position
import space.dawdawich.strategy.model.Trend
import space.dawdawich.utils.plusPercent
import java.util.UUID
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
    private val priceMinStep: Double,
    simulateTradeOperations: Boolean,
    middlePrice: Double = 0.0,
    moneyChangePostProcessFunction: MoneyChangePostProcessFunction,
    updateMiddlePrice: UpdateMiddlePricePostProcessFunction = { _ -> },
    private val createOrderFunction: CreateOrderFunction = { inPrice: Double, orderSymbol: String, qty: Double, refreshTokenUpperBorder: Double, refreshTokenLowerBorder: Double, trend: Trend ->
        Order(inPrice, orderSymbol, qty, refreshTokenUpperBorder, refreshTokenLowerBorder, trend)
    },
    private var minPrice: Double = -1.0,
    private var maxPrice: Double = -1.0,
    private var step: Double = 0.0,
    id: String = UUID.randomUUID().toString()
) : StrategyRunner(
    money,
    multiplier,
    moneyChangePostProcessFunction,
    symbol,
    simulateTradeOperations,
    id
) {
    private var priceOutOfDiapasonCounter = 0
    private var middlePrice: Double by Delegates.observable(middlePrice) { _, _, newValue ->
        updateMiddlePrice(newValue)
    }
    private val orderPriceGrid: MutableMap<Double, Order?> = mutableMapOf()

    fun fillOrder(orderId: String) {
        orderPriceGrid.values.filterNotNull().find { order -> order.id == orderId }?.isFilled = true
    }

    fun setDiapasonConfigs(minPrice: Double, maxPrice: Double, step: Double) {
        this.minPrice = minPrice
        this.maxPrice = maxPrice
        this.step = step
    }

    override fun getRuntimeInfo() = GridTableStrategyRuntimeInfoModel(
        id,
        orderPriceGrid.entries.map { "${it.key}=${if (it.value != null) "true" else "false"}" },
        currentPrice,
        middlePrice,
        position?.convertToInfo()
    )

    override fun getStrategyConfig() =
        GridStrategyConfigModel(id, symbol, money, multiplier, diapason, gridSize, stopLoss, takeProfit, priceMinStep, middlePrice, minPrice, maxPrice, step, orderPriceGrid.keys)

    override fun acceptPriceChange(previousPrise: Double, currentPrice: Double) {
        this.currentPrice = currentPrice

        if (simulateTradeOperations) {
            if (minPrice <= 0.0) {
                setUpPrices(currentPrice)
            }

            checkPriceForSetupBounds(currentPrice)
        }

        processOrders(currentPrice, previousPrise)

        if (simulateTradeOperations) {
            position?.let { position ->
                if (position.size > 0.0) {
                    moneyWithProfit =
                        money + position.calculateProfit(currentPrice) // Process data to update in db including calculation profits/loses
                    if (moneyWithProfit > money.plusPercent(takeProfit) || moneyWithProfit < money.plusPercent(-stopLoss)) {
                        money = moneyWithProfit
                        this.position = null
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
        orderPriceGrid.putAll(gridPrices.map {
            it to null
        })
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
        val moneyPerPosition = money / gridSize

        orderPriceGrid.entries
            .asSequence()
            .filter { (it.key - currentPrice).absoluteValue > priceMinStep }
            .sortedBy { (it.key - currentPrice).absoluteValue }
            .take(2)
            .filter { it.value == null }
            .map { it.key }
            .toList()
            .forEach { inPrice ->
                val isLong = if (inPrice < middlePrice) Trend.LONG else Trend.SHORT
                val qty = moneyPerPosition * multiplier / inPrice

                if (position?.let { pos ->
                        val prof = if (pos.trend == Trend.LONG) inPrice - pos.entryPrice else pos.entryPrice - inPrice
                        (prof - pos.entryPrice * 0.00055 - inPrice * 0.00055) * qty > 0
                    } == false) {
                    return@forEach
                }

                if ((position?.getPositionValue() ?: 0.0) / multiplier + step > money) {
                    return@forEach
                }

                orderPriceGrid[inPrice] = createOrderFunction(
                    inPrice,
                    symbol,
                    qty,
                    inPrice - step * isLong.direction,
                    inPrice + step * isLong.direction,
                    isLong
                )
            }

        orderPriceGrid.values.filterNotNull().forEach { order ->
            if (!order.isFilled && simulateTradeOperations) {
                order.isFilled =
                    (order.inPrice > previousPrice && order.inPrice <= currentPrice) ||
                            (order.inPrice < previousPrice && order.inPrice >= currentPrice)
                if (order.isFilled) {
                    position?.updateSizeAndEntryPrice(order) ?: run {
                        position = Position(order.inPrice, order.count, order.trend)
                    }
                    position?.let { if (it.size <= 0) position = null }
                }
            } else if (order.isPriceOutOfRefreshBorder(currentPrice)) {
                order.isFilled = false
                orderPriceGrid[orderPriceGrid.entries.first { it.value == order }.key] = null
            }
        }
    }
}
