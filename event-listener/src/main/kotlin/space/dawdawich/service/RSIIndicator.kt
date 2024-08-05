package space.dawdawich.service

import space.dawdawich.utils.calculatePercentageChange

class RSIIndicator(private val period: Int = 14) : java.io.Serializable {
    private var averageGain: Double = 0.0
    private var averageLoss: Double = 0.0
    private var prevClose: Double = Double.NaN
    private var count: Int = 0
    private var rsi = Double.NaN

    fun updateRSI(closePrice: Double): Double {
        rsi = addKLine(closePrice)
        return rsi
    }

    private fun addKLine(closePrice: Double): Double {
        if (prevClose.isNaN()) {
            prevClose = closePrice
            return Double.NaN
        }

        val change = prevClose.calculatePercentageChange(closePrice)
        val gain = if (change > 0) change else 0.0
        val loss = if (change < 0) -change else 0.0

        if (count < period) {
            // Initial calculations for the first `period` klines
            averageGain += gain
            averageLoss += loss
            if (++count == period) {
                // At this point, we have enough data to calculate the first RSI
                averageGain /= period
                averageLoss /= period
            }
        } else {
            // Subsequent calculations use the smoothed average
            averageGain = (averageGain * (period - 1) + gain) / period
            averageLoss = (averageLoss * (period - 1) + loss) / period
        }

        prevClose = closePrice

        return if (averageLoss == 0.0) {
            100.0
        } else {
            val rs = averageGain / averageLoss
            100 - (100 / (1 + rs))
        }
    }

    fun calculateRSI(closePrice: Double): Double {
        if (prevClose.isNaN() || count < period) {
            return Double.NaN
        }

        val change = prevClose.calculatePercentageChange(closePrice)
        val gain = if (change > 0) change else 0.0
        val loss = if (change < 0) -change else 0.0

        // Subsequent calculations use the smoothed average
        val averageGain = (averageGain * (period - 1) + gain) / period
        val averageLoss = (averageLoss * (period - 1) + loss) / period

        return if (averageLoss == 0.0) {
            100.0
        } else {
            val rs = averageGain / averageLoss
            100 - (100 / (1 + rs))
        }
    }
}
