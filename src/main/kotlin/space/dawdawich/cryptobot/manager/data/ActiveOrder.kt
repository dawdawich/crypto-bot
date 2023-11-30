package space.dawdawich.cryptobot.manager.data

import space.dawdawich.cryptobot.data.Trend

data class ActiveOrder(
    val id: String,
    val openPositionPrice: Double,
    val takeProfit: Double,
    val multiplier: Int,
    val pair: String,
    var qty: Double,
    var stopLoss: Double,
    val trend: Trend,
    var isClosed: Boolean = false,
    var isFilled: Boolean = false,
    var watchdog: Double = 0.0
)
