package space.dawdawich.client.responses

import kotlinx.serialization.Serializable

@Serializable
data class PositionResponse(
    val symbol: String,
    val side: String,
    val size: String,
    val entryPrice: String,
    val updatedTime: String
)
