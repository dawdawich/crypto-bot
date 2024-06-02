package space.dawdawich.controller.model

import kotlinx.serialization.Serializable

/**
 * Represents the response for symbol information.
 *
 * @property symbol The symbol.
 * @property partition The partition.
 * @property minPrice The minimum price.
 * @property maxPrice The maximum price.
 * @property tickSize The tick size.
 * @property minOrderQty The minimum order quantity.
 * @property maxOrderQty The maximum order quantity.
 * @property qtyStep The quantity step.
 */
@Serializable
data class SymbolResponse(
    val symbol: String,
    val partition: Int,
    val minPrice: Double,
    val maxPrice: Double,
    val tickSize: Double,
    val minOrderQty: Double,
    val maxOrderQty: Double,
    val qtyStep: Double
)
