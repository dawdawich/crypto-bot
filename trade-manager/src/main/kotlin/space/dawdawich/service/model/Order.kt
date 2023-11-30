package space.dawdawich.service.model

data class Order(
    val symbol: String,
    val isLong: Boolean,
    val price: Double,
    val qty: Double,
    val orderStatus: String,
    val orderLinkId: String
)
