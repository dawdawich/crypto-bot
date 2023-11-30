package space.dawdawich.service.model

class Position(val symbol: String, val isLong: Boolean, var size: Double, var entryPrice: Double, var updateTime: Long) {

    fun calculateProfit(currentPrice: Double): Double {
        val profitPerUnit = if (!isLong) currentPrice - entryPrice else entryPrice - currentPrice
        return (profitPerUnit - currentPrice * 0.00055) * size
    }
}
