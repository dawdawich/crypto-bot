package space.dawdawich.strategy.model

import java.util.*

data class Order(
    var inPrice: Double,
    val pair: String,
    val count: Double,
    var refreshTokenUpperBorder: Double,
    var refreshTokenLowerBorder: Double,
    val trend: Trend,
    var isFilled: Boolean = false,
    val createTime: Long = System.currentTimeMillis(),
    var id: String = UUID.randomUUID().toString()
) : java.io.Serializable {
    fun isPriceOutOfRefreshBorder(currentPrice: Double): Boolean =
        (trend == Trend.LONG && (currentPrice <= refreshTokenUpperBorder || currentPrice >= refreshTokenLowerBorder)) ||
                (trend == Trend.SHORT && (currentPrice >= refreshTokenUpperBorder || currentPrice <= refreshTokenLowerBorder))
}
