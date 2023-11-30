package space.dawdawich.cryptobot.service.reuests

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class OrderDataRequest @OptIn(ExperimentalSerializationApi::class) constructor(
    val symbol: String,
    val side: String,
    val orderType: String,
    val qty: String,
    var stopLoss: String,
    var takeProfit: String,
    val triggerPrice: String,
    val triggerDirection: Int,
    val timeInForce: String = "IOC",
    @EncodeDefault val triggerBy: String = "MarkPrice",
    @EncodeDefault val category: String = "linear",
    @EncodeDefault val isLeverage: Int = 1,
    @EncodeDefault val positionIdx: Int = 0,
    @EncodeDefault @SerialName("orderLinkId") val id: String = UUID.randomUUID().toString(),
) {
}
