package space.dawdawich.strategy.model

import java.util.*

open class Order(
    var inPrice: Double,
    val pair: String,
    val count: Double,
    val trend: Trend,
    var isFilled: Boolean = false,
    val createTime: Long = System.currentTimeMillis(),
    var id: String = UUID.randomUUID().toString()
)
