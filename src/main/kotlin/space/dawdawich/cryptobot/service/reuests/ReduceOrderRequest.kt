package space.dawdawich.cryptobot.service.reuests

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class ReduceOrderRequest @OptIn(ExperimentalSerializationApi::class) constructor(
    val symbol: String,
    val side: String,
    @SerialName("order_type") val orderType: String,
    val qty: String,
    var positionIdx: Int,
    @EncodeDefault val reduceOnly: Boolean = true,
    @EncodeDefault val closeOnTrigger: Boolean = true,
    @EncodeDefault val category: String = "linear",
    @EncodeDefault @SerialName("orderLinkId") val id: String = UUID.randomUUID().toString(),
)
