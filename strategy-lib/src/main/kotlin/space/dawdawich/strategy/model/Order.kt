package space.dawdawich.strategy.model

data class Order(
    var inPrice: Double,
    val qty: Double,
    val trend: Trend
)
