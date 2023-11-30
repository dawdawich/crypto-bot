package space.dawdawich.cryptobot.client.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderResponse(
    @SerialName("orderLinkId") val id: String,
    val orderStatus: String,
    val triggerPrice: String,
    val takeProfit: String,
    val stopLoss: String
)
