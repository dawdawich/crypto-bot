package space.dawdawich.controller.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateSymbolRequest(val symbol: String)
