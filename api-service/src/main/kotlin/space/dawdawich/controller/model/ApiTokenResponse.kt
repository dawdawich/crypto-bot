package space.dawdawich.controller.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiTokenResponse(val id: String, val apiKey: String, val market: String, val test: Boolean)
