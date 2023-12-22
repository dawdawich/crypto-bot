package space.dawdawich.controller.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateTradeManagerRequest(val apiTokenId: String, val active: Boolean, val customAnalyzerId: String)
