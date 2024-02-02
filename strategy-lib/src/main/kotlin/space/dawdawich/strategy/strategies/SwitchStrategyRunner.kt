package space.dawdawich.strategy.strategies

import space.dawdawich.model.strategy.runtimeModel.StrategyRuntimeInfoModel
import space.dawdawich.model.strategy.configModel.SwitchStrategyConfigModel
import space.dawdawich.model.strategy.runtimeModel.SwitchStrategyRuntimeInfoModel
import space.dawdawich.strategy.StrategyRunner
import space.dawdawich.strategy.model.*
import java.util.*

class SwitchStrategyRunner(
        symbol: String,
        multiplier: Int,
        money: Double,
        simulateTradeOperations: Boolean,
        moneyChangePostProcessFunction: MoneyChangePostProcessFunction,
        id: String = UUID.randomUUID().toString(),
        priceMinStep: Double,
        minQtyStep: Double,
        private val capitalOrderPerPercent: Int,
        private val switchCounterValue: Int,
        private val coefficientBetweenOrders: Double,
        private val createSwitchOrderFunction: CreateSwitchOrderFunction = { inPrice: Double, orderSymbol: String, qty: Double, trend: Trend ->
            Order(inPrice, orderSymbol, qty, trend)
        },
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
    private var direction: Trend? = null
    private var counter: Int = 0
    private var previousOrder: Order? = null

    @Synchronized
    override fun acceptPriceChange(previousPrice: Double, currentPrice: Double) {
        defineTrendDependOnPrices(previousPrice, currentPrice)
                .run { updatePosition(previousPrice, currentPrice) }
                .run { createOrder(currentPrice) }
    }

    override fun getRuntimeInfo(): StrategyRuntimeInfoModel =
            SwitchStrategyRuntimeInfoModel(
                    id,
                    currentPrice,
                    position?.convertToInfo(),
                    direction, // TODO
                    counter
            )

    override fun getStrategyConfig() =
            SwitchStrategyConfigModel(
                    id,
                    symbol,
                    money,
                    multiplier,
                    priceMinStep,
                    minQtyStep,
                    capitalOrderPerPercent,
                    switchCounterValue,
                    coefficientBetweenOrders
            )

    private fun defineTrendDependOnPrices(previousPrise: Double, currentPrice: Double) {
        if (currentPrice > previousPrise) {
            counter++
        } else if (currentPrice < previousPrise) {
            counter--
        }

        if (counter >= switchCounterValue) {
            counter = switchCounterValue
            direction = Trend.LONG
        } else if (counter <= -switchCounterValue) {
            counter = -switchCounterValue
            direction = Trend.SHORT
        }
    }

    private fun updatePosition(previousPrise: Double, currentPrice: Double) {
        previousOrder?.let {
            if (!it.isFilled && simulateTradeOperations) {
                it.isFilled = isOrderFilled(it, previousPrise, currentPrice)
                if (it.isFilled) {
                    position?.updateSizeAndEntryPrice(it) ?: run { position = Position(it.inPrice, it.count, it.trend) }
                    position?.let { pos -> if (pos.size <= 0) position = null }
                }
            }
        }
    }

    private fun isOrderFilled(order: Order, previousPrise: Double, currentPrice: Double): Boolean =
            (order.inPrice > previousPrise && order.inPrice <= currentPrice) || (order.inPrice < previousPrise && order.inPrice >= currentPrice)

    private fun createOrder(currentPrice: Double) {
        if (comparePositionPriceAndCurrentPriceDependOnTrend(currentPrice)) {
            direction?.let { trend ->
                val inPrice = previousOrder?.let { it.inPrice * (1 + coefficientBetweenOrders / 100) } ?: currentPrice
                createSwitchOrderFunction(
                        inPrice,
                        symbol,
                        multiplier * money * capitalOrderPerPercent / 100,
                        trend
                )?.let { order -> previousOrder = order }
            }
        }
    }

    private fun comparePositionPriceAndCurrentPriceDependOnTrend(currentPrice: Double): Boolean {
        position?.let {
            if (it.trend != direction) {
                if (direction == Trend.SHORT) {
                    return currentPrice > it.entryPrice
                } else if (direction == Trend.LONG) {
                    return currentPrice < it.entryPrice
                }
            }
        }
        return true
    }

}
