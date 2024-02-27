package space.dawdawich.integration.model

import kotlinx.serialization.Serializable

@Serializable
data class PairInfo(val minPrice: Double, val maxPrice: Double, val tickSize: Double, val minOrderQty: Double, val maxOrderQty: Double, val qtyStep: Double, val maxLeverage: Double) {
    constructor(params: Array<Double>) : this(params[0], params[1], params[2], params[3], params[4], params[5], params[6])
}
