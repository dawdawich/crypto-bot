package space.dawdawich.integration.model

import kotlinx.serialization.Serializable

@Serializable
data class PairInfo(val minPrice: Double, val maxPrice: Double, val minOrderQty: Double, val maxOrderQty: Double, val leverageStep: Double, val maxLeverage: Double, val qtyStep: Double, val launchTime: Long, val name: String) {
    constructor(params: Array<Double>, name: String) : this(params[0], params[1], params[2], params[3], params[4], params[5], params[6], params[7].toLong(), name)
}
