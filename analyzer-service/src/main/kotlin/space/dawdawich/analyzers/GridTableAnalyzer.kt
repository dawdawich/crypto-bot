package space.dawdawich.analyzers

import space.dawdawich.analyzers.helper.PositionManager
import space.dawdawich.data.Order
import space.dawdawich.data.Trend
import space.dawdawich.model.analyzer.GridTableDetailInfoModel
import space.dawdawich.model.analyzer.PositionModel
import space.dawdawich.utils.plusPercent
import java.util.*
import kotlin.math.absoluteValue
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class GridTableAnalyzer(
    private val diapason: Int,
    private val gridSize: Int,
    private var money: Double,
    private val multiplier: Int,
    private val stopLoss: Int,
    private val takeProfit: Int,
    val symbol: String,
    isOneWayMode: Boolean,
    private val priceMinStep: Double,
    val id: String = UUID.randomUUID().toString(),
    moneyChangeFunction: (KProperty<*>, Double, Double) -> Unit = { _, _, _ -> },
    private val updateMiddlePrice: (Double) -> Unit = { _ -> }
) {
    private var currentPrice = 0.0
    private var minPrice: Double = -1.0
    private var maxPrice: Double = -1.0
    private var outOfBoundCounter = 0
    private var middlePrice by Delegates.observable(0.0) { _, _, newValue ->
        updateMiddlePrice(newValue)
    }
    private val positionManager: PositionManager =
        PositionManager(isOneWayMode, stopLoss.toDouble(), takeProfit.toDouble())

    private var orderPriceGrid: MutableMap<Double, Order?> = mutableMapOf()

    private var moneyWithProfit: Double by Delegates.observable(money) { _, _, newValue ->
        if ((newValue - moneyHandler).absoluteValue > moneyHandler * 0.01) {
            moneyHandler = newValue
        }
    }
    private var moneyHandler: Double by Delegates.observable(money, moneyChangeFunction)

    fun getInfo() = GridTableDetailInfoModel(
        id,
        orderPriceGrid.map { (key, value) -> "$key=${value != null}" },
        currentPrice,
        middlePrice,
        positionManager.getPositions().map { PositionModel(it.trend.direction > 0, it.size, it.entryPrice) })

    fun acceptPriceChange(previousPrice: Double, currentPrice: Double) {
        this.currentPrice = currentPrice
        if (minPrice <= 0.0) {
            setUpPrices(currentPrice)
        }

        checkPriceForResetOrders(currentPrice)

        processOrders(currentPrice, previousPrice)

        positionManager.getPositions().filter { position ->
            if (position.size > 0.0) {
                moneyWithProfit =
                    money + position.calculateProfit(currentPrice) // Process data to update in db including calculation profits/loses
                return@filter moneyWithProfit > money.plusPercent(takeProfit) || moneyWithProfit < money.plusPercent(-stopLoss)
            }
            return@filter false
        }.forEach {
            money += positionManager.resetPositions(currentPrice, it.trend)
        }
    }

    private fun checkPriceForResetOrders(currentPrice: Double) {
        if (currentPrice !in minPrice..maxPrice && outOfBoundCounter++ > 30) {
            reinitializeBounds(currentPrice)
            outOfBoundCounter = 0
        } else if (currentPrice in minPrice..maxPrice) {
            outOfBoundCounter = 0
        }
    }

    private fun reinitializeBounds(currentPrice: Double) {
        money += positionManager.resetPositions(currentPrice, Trend.LONG)
        money += positionManager.resetPositions(currentPrice, Trend.SHORT)

        setUpPrices(currentPrice)
    }

    private fun setUpPrices(currentPrice: Double) {
        minPrice = currentPrice.plusPercent(-diapason)
        maxPrice = currentPrice.plusPercent(diapason)
        setupGrid(currentPrice)
    }

    private fun processOrders(currentPrice: Double, previousPrice: Double) {
        val nearOrders = orderPriceGrid.entries.filter { (it.key - currentPrice).absoluteValue > priceMinStep }
            .sortedBy { (it.key - currentPrice).absoluteValue }.take(2)

        nearOrders.filter { it.value == null }.forEach {
            val moneyPerPosition = money / gridSize

            val isLong = if (it.key < (maxPrice + minPrice) / 2) Trend.LONG else Trend.SHORT
            val inPrice = it.key
            val step = (maxPrice - minPrice) / gridSize

            val qty = moneyPerPosition * multiplier / inPrice

            if (positionManager.isOneWay()) {
                positionManager.getPositions().firstOrNull { it.trend != isLong && it.size > 0.0 }?.let { pos ->
                    val prof = if (pos.trend == Trend.LONG) it.key - pos.entryPrice else pos.entryPrice - it.key
                    (prof - pos.entryPrice * 0.00055 - it.key * 0.00055) * qty > 0
                }
            } else {
                null
            }?.let { higherThanZero -> if (!higherThanZero) return@forEach }

            if (positionManager.getPositionsValue() / multiplier + 0.2 > money) {
                return@forEach
            }

            orderPriceGrid[it.key] = Order(
                it.key,
                symbol,
                qty,
                it.key - step * isLong.direction,
                it.key + step * isLong.direction,
                isLong
            )
        }

        orderPriceGrid.values.filterNotNull().forEach { order ->
            if (!order.isFilled) {
                order.isFilled =
                    (order.trend == Trend.LONG && order.inPrice < currentPrice && order.inPrice >= previousPrice) ||
                            (order.trend == Trend.SHORT && order.inPrice > currentPrice && order.inPrice <= previousPrice)
                if (order.isFilled) {
                    val orderToAdd =
                        order.copy(count = money / gridSize * multiplier / order.inPrice) // apply price regarding current balance
                    if (positionManager.getPositionsValue() / multiplier < money) {
                        positionManager.updatePosition(orderToAdd)
                    }
                }
            } else if (order.isTakeProfitExceeded(currentPrice) || order.isStopLossExceeded(currentPrice)) {
                order.isFilled = false
                orderPriceGrid[orderPriceGrid.entries.first { it.value == order }.key] = null
            }
        }
    }

    private fun setupGrid(currentPrice: Double) {
        val step = (maxPrice - minPrice) / gridSize

        var minPrice = currentPrice
        var maxPrice = currentPrice
        val gridPrices = mutableListOf<Double>()

        minPrice -= step
        repeat(gridSize / 2) {
            minPrice -= step
            gridPrices += minPrice
        }
        maxPrice += step
        repeat(gridSize / 2) {
            maxPrice += step
            gridPrices += maxPrice
        }

        orderPriceGrid = mutableMapOf(*gridPrices.map {
            it to null
        }.toTypedArray())
        middlePrice = currentPrice
    }
}
