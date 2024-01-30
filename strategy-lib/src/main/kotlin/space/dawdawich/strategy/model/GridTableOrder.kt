package space.dawdawich.strategy.model

import java.util.*

class GridTableOrder(
        inPrice: Double,
        pair: String,
        count: Double,
        private val refreshTokenUpperBorder: Double,
        private val refreshTokenLowerBorder: Double,
        trend: Trend,
        isFilled: Boolean = false,
        createTime: Long = System.currentTimeMillis(),
        id: String = UUID.randomUUID().toString()
) : Order(
        inPrice,
        pair,
        count,
        trend,
        isFilled,
        createTime,
        id) {

    fun isPriceOutOfRefreshBorder(currentPrice: Double): Boolean =
            (trend == Trend.LONG && (currentPrice <= refreshTokenUpperBorder || currentPrice >= refreshTokenLowerBorder)) ||
                    (trend == Trend.SHORT && (currentPrice >= refreshTokenUpperBorder || currentPrice <= refreshTokenLowerBorder))
}
