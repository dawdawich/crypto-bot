package space.dawdawich.controller.model

import kotlinx.serialization.Serializable

@Serializable
data class IdListRequest(val ids: List<String>)
