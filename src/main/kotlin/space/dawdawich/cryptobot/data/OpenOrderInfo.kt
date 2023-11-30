package space.dawdawich.cryptobot.data

data class OpenOrderInfo(
    val trend: Trend,
    val stopLoss: Float,
    val takeProfit: Float,
    val multiplier: Int,
    val currentPrice: Double
)
