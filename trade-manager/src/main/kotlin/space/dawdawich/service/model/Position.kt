package space.dawdawich.service.model

import space.dawdawich.utils.plusPercent

data class Position(val symbol: String, val isLong: Boolean, var size: Double, var entryPrice: Double, val positionIdx: Int, var updateTime: Long) {

    fun calculateProfit(currentPrice: Double): Double {
        val profitPerUnit = if (isLong) currentPrice - entryPrice else entryPrice - currentPrice
        return (profitPerUnit - currentPrice * 0.00055 - entryPrice * 0.00055) * size
    }

    fun isTpOrSlCrossed(currentPrice: Double, sl: Double, tp: Double): Boolean {
        val direction = if (isLong) 1 else -1
        val tpPrice = entryPrice.plusPercent(tp * direction)
        val slPrice = entryPrice.plusPercent(-sl * direction)


        val isTpCrossed = when (isLong) {
            true -> {
                currentPrice >= tpPrice
            }

            else -> {
                currentPrice <= tpPrice
            }
        }

        val isSlCrossed = when (isLong) {
            true -> {
                currentPrice <= slPrice
            }

            else -> {
                currentPrice >= slPrice
            }
        }

        return isTpCrossed || isSlCrossed
    }
}
