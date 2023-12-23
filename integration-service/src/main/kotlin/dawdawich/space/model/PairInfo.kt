package dawdawich.space.model

import kotlinx.serialization.Serializable

@Serializable
data class PairInfo(val minPrice: Double, val maxPrice: Double, val tickSize: Double, val minOrderQty: Double, val maxOrderQty: Double, val qtyStep: Double)
