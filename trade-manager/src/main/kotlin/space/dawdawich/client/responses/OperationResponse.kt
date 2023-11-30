package space.dawdawich.client.responses

import kotlinx.serialization.SerialName

data class OperationResponse(
    val operation: String = "op",
    val success: Boolean,
    @SerialName("conn_id") val connectionId: String
)
