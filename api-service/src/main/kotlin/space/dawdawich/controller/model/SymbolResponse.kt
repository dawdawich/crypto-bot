package space.dawdawich.controller.model

import kotlinx.serialization.Serializable

@Serializable
data class SymbolResponse(
    val symbol: String,
    val partition: Int,
    val isOneWayMode: Boolean,
    val minPrice: Double,
    val maxPrice: Double,
    val tickSize: Double,
    val minOrderQty: Double,
    val maxOrderQty: Double,
    val qtyStep: Double
)
