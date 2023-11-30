package space.dawdawich.client.responses

import kotlinx.serialization.Serializable

@Serializable
class OrderResponse(
    val symbol: String,
    val side: String,
    val price: String,
    val qty: String,
    val orderStatus: String,
    val orderLinkId: String
) {
}
