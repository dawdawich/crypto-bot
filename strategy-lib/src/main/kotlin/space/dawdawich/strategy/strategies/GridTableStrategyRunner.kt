package space.dawdawich.strategy.strategies

import space.dawdawich.strategy.PriceChangeStrategyRunner
import space.dawdawich.strategy.model.*
import space.dawdawich.utils.plusPercent
import space.dawdawich.utils.trimToStep
import java.util.*

class GridTableStrategyRunner(
    currentPrice: Double,
    money: Double,
    multiplier: Double,
    symbol: String,
    diapason: Int,
    gridSize: Int,
    minPriceStep: Double,
    minQtyStep: Double,
    id: String = UUID.randomUUID().toString(),
) : PriceChangeStrategyRunner(
    money,
    multiplier,
    symbol,
    minQtyStep,
    id
) {
    private val orderPriceGrid: MutableMap<Double, Order?> = mutableMapOf()
    private val step: Double
    private val middlePrice: Double
    private val qtyPerOrder: Double
    private var lastPrice: Double = currentPrice

    private var realizedPnL = 0.0

    init {
        val step = ((currentPrice.plusPercent(diapason) - currentPrice.plusPercent(-diapason)) / gridSize).let {
            if (it < minPriceStep) minPriceStep else it
        }

//        if (step < minPriceStep) throw IllegalArgumentException(
//            "Cannot create grid runner for symbol '$symbol'. " +
//                    "Diapason '$diapason' and grid size '$gridSize'. " +
//                    "Calculated step '$step' less than min price step '$minPriceStep'."
//        )

        val gridPrices = mutableListOf<Double>()
        var minPrice = currentPrice
        var maxPrice = currentPrice

        gridPrices += currentPrice
        minPrice -= step
        maxPrice += step
        repeat(gridSize / 2) {
            minPrice -= step
            maxPrice += step
            gridPrices += minPrice
            gridPrices += maxPrice
        }

        orderPriceGrid += gridPrices.map { it to null }
        middlePrice = currentPrice
        this.step = step

        this.qtyPerOrder = (money * multiplier / ((gridSize + 1) * currentPrice)).trimToStep(minQtyStep).let { if (it < minQtyStep) minQtyStep else it }
    }

    override fun acceptPriceChange(currentPrice: Double) {
        processOrdersToStart(this.lastPrice, currentPrice)
        processOrdersToFinish(this.lastPrice, currentPrice)

        this.lastPrice = currentPrice
    }

    override fun getPnL(): Double {
        return realizedPnL + getUnrializedPnL()
    }

    override fun getUnrializedPnL(): Double {
        val openOrders = orderPriceGrid.values.filterNotNull()
        val longOrders = openOrders.filter { it.trend == Trend.LONG }
        val shortOrders = openOrders.filter { it.trend == Trend.SHORT }
        val longQty = longOrders.sumOf { it.qty }
        val shortQty = shortOrders.sumOf { it.qty }

        // Long positions
        val longUnrealized = if (longQty > 0) {
            // Weighted average entry price for longs
            val longEntrySum = longOrders.sumOf { it.qty * it.inPrice }
            val avgLongEntry = longEntrySum / longQty
            (lastPrice - avgLongEntry) * longQty
        } else 0.0

        // Short positions
        val shortUnrealized = if (shortQty > 0) {
            // Weighted average entry price for shorts
            val shortEntrySum = shortOrders.sumOf { it.qty * it.inPrice }
            val avgShortEntry = shortEntrySum / shortQty
            (avgShortEntry - lastPrice) * shortQty
        } else 0.0

        return longUnrealized + shortUnrealized
    }

    private fun processOrdersToStart(previousPrice: Double, currentPrice: Double) = orderPriceGrid.entries
        .asSequence()
        .filter { it.value == null }
        .filter { it.key != middlePrice }
        .filter { it.key in previousPrice..currentPrice || it.key in currentPrice..previousPrice }
        .map { it.key }
        .forEach { orderPrice ->
            val orderTrend = if (orderPrice < middlePrice) Trend.LONG else Trend.SHORT

            orderPriceGrid[orderPrice] = Order(orderPrice, qtyPerOrder, orderTrend)
        }

    private fun processOrdersToFinish(previousPrice: Double, currentPrice: Double) = orderPriceGrid.entries
        .asSequence()
        .filter { it.value != null }
        .filter {
            it.value!!.trend == Trend.LONG && it.value!!.inPrice + step in previousPrice..currentPrice
                    ||
                    it.value!!.trend == Trend.SHORT && it.value!!.inPrice - step in currentPrice..previousPrice
        }
        .map {
            val closePrice =
                if (it.value!!.trend == Trend.LONG) it.value!!.inPrice + step else it.value!!.inPrice - step
            val fee = 0.00055 * qtyPerOrder * (closePrice + it.value!!.inPrice)
            realizedPnL += step * qtyPerOrder - fee

            it.key
        }
        .forEach { orderPriceGrid[it] = null }
}
