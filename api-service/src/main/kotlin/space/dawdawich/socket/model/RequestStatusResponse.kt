package space.dawdawich.socket.model

import kotlinx.serialization.Serializable
import space.dawdawich.repositories.mongo.entity.RequestStatus

@Serializable
data class RequestStatusResponse(
    val status: RequestStatus,
)
