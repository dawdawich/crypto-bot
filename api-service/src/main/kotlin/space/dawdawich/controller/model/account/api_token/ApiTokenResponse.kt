package space.dawdawich.controller.model.account.api_token

import kotlinx.serialization.Serializable

@Serializable
data class ApiTokenResponse(val id: String, val apiKey: String, val market: String, val test: Boolean)
