package space.dawdawich.strategy.strategies

import mu.KotlinLogging
import space.dawdawich.model.strategy.CandleTailStrategyConfigModel
import space.dawdawich.model.strategy.CandleTailStrategyRuntimeInfoModel
import space.dawdawich.model.strategy.StrategyRuntimeInfoModel
import space.dawdawich.strategy.KLineStrategyRunner
import space.dawdawich.strategy.model.*
import kotlin.math.abs

class CandleTailStrategyRunner(
    money: Double,
    multiplier: Int,
    symbol: String,
    simulateTradeOperations: Boolean,
    kLineDuration: Int,
    private val stopLoss: Int,
    private val takeProfit: Int,
    minQtyStep: Double,
    id: String,
    moneyChangeFunction: MoneyChangePostProcessFunction = { _, _ -> },
    createOrderFunction: CreateOrderFunction = { inPrice: Double, orderSymbol: String, qty: Double, refreshTokenUpperBorder: Double, refreshTokenLowerBorder: Double, trend: Trend ->
        Order(inPrice, orderSymbol, qty, refreshTokenUpperBorder, refreshTokenLowerBorder, trend)
    },
    cancelOrderFunction: CancelOrderFunction = { _, _ -> true },
) : KLineStrategyRunner(money, multiplier, moneyChangeFunction, createOrderFunction, cancelOrderFunction, minQtyStep, symbol, simulateTradeOperations, kLineDuration, id) {
    private val logger = KotlinLogging.logger {}

    override fun acceptKLine(kLine: KLine) {
        if (money > 0) {
            val body = abs(kLine.closePrice - kLine.openPrice)
            val lowerShadow = kLine.openPrice.coerceAtMost(kLine.closePrice) - kLine.lowPrice
            val upperShadow = kLine.highPrice - kLine.openPrice.coerceAtLeast(kLine.closePrice)
            var totalRange = upperShadow + body + lowerShadow
            if (totalRange == 0.0) {
                totalRange = 1.0
            }
            var moneyToUse = money - ((position?.getPositionValue() ?: 0.0) / multiplier)

            logger.debug { "Order creation info: body - $body, lowerShadow - $lowerShadow, upperShadow - $upperShadow, moneyToUse - $moneyToUse, money - $money" }
            val order = if (lowerShadow != 0.0 && lowerShadow > upperShadow && (position?.trend?.equals(Trend.LONG) != false)) {

                moneyToUse = (if (position?.trend?.equals(Trend.LONG) != false) moneyToUse else money) * (lowerShadow / totalRange).coerceAtMost(0.7)
                createOrderFunction(
                    kLine.closePrice,
                    symbol,
                    (moneyToUse / kLine.closePrice) * multiplier,
                    -1.0,
                    -1.0,
                    Trend.LONG
                )
            } else if (upperShadow != 0.0 && lowerShadow < upperShadow) {

                moneyToUse = (if (position?.trend?.equals(Trend.SHORT) != false) moneyToUse else money) * (upperShadow / totalRange).coerceAtMost(0.7)
                createOrderFunction(
                    kLine.closePrice,
                    symbol,
                    (moneyToUse / kLine.closePrice) * multiplier,
                    -1.0,
                    -1.0,
                    Trend.SHORT
                )
            } else {
                null
            }

            if (order != null && simulateTradeOperations) {
                if (position == null) {
                    position = Position(order.inPrice, order.count, order.trend)
                } else if (order.trend == position!!.trend) {
                    position!!.updateSizeAndEntryPrice(order)
                }
            }
        }
    }

    override fun acceptPriceChange(previousPrise: Double, currentPrice: Double) {
        checkPosition(currentPrice)
    }

    override fun getStrategyConfig() = CandleTailStrategyConfigModel(id, symbol, money, multiplier, stopLoss, takeProfit, kLineDuration, minQtyStep)
    override fun getRuntimeInfo(): StrategyRuntimeInfoModel = CandleTailStrategyRuntimeInfoModel(id, position?.convertToInfo())

    private fun checkPosition(currentPrice: Double) {
        if (money > 0) {
            position?.let {
                val profit = it.calculateProfit(currentPrice)
                moneyWithProfit += profit
                if (profit <= -stopLoss) {
                    closePositionFunction(true)
                    money += profit
                } else if (profit >= takeProfit) {
                    closePositionFunction(false)
                    money += profit
                }
            }
        } else {
            money = 0.0
        }
    }
}
