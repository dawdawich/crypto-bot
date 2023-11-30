package space.dawdawich.client.request

import kotlinx.serialization.SerialName

data class OperationRequest(
    @SerialName("op")
    val operation: String = "auth",
    @SerialName("args")
    val arguments: List<Any>
)
