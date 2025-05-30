package space.dawdawich.managers

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import space.dawdawich.integration.client.PrivateHttpClient
import space.dawdawich.model.strategy.CandleTailStrategyConfigModel
import space.dawdawich.model.strategy.StrategyConfigModel
import space.dawdawich.strategy.model.CreateOrderFunction
import space.dawdawich.strategy.model.Order
import space.dawdawich.strategy.model.Trend
import space.dawdawich.utils.trimToStep
import java.util.*
val logger = KotlinLogging.logger {}
fun getCreateOrderFunction(strategyConfig: StrategyConfigModel, marketService: PrivateHttpClient, repeatCount: Int = 1, isLimitOrder: Boolean = true): CreateOrderFunction {
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
        logger.info { "Try to place order: inPrice - $inPrice, symbol - $orderSymbol, qty - $qty, trend - $trend}" + if (strategyConfig is CandleTailStrategyConfigModel) ", interval - ${strategyConfig.kLineDuration}" else "" }
        val isSuccess =
            runBlocking {
                marketService.createOrder(
                    orderSymbol,
                    inPrice,
                    orderQty,
                    trend.directionBoolean,
                    orderId,
                    repeatCount = repeatCount,
                    isLimitOrder = isLimitOrder
                )
            }
        if (isSuccess) {
            Order(inPrice, orderSymbol, qty, refreshTokenUpperBorder, refreshTokenLowerBorder, trend, id = orderId)
        } else {
            null
        }
    }
    return createOrderFunction
}
