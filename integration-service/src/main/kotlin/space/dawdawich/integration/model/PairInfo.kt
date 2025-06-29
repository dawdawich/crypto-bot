package space.dawdawich.integration.model

import kotlinx.serialization.Serializable
import space.dawdawich.model.Market

@Serializable
data class PairInfo(val minPrice: Double, val maxPrice: Double, val minOrderQty: Double, val maxOrderQty: Double, val leverageStep: Double, val maxLeverage: Double, val qtyStep: Double, val launchTime: Long, val name: String, val market: Market) {
    constructor(params: Array<Double>, name: String, market: Market) : this(params[0], params[1], params[2], params[3], params[4], params[5], params[6], params[7].toLong(), name, market)
}
