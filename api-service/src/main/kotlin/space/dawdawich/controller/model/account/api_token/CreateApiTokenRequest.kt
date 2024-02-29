package space.dawdawich.controller.model.account.api_token

import kotlinx.serialization.Serializable

@Serializable
data class CreateApiTokenRequest(val market: String, val apiKey: String, val secretKey: String, val demo: Boolean)
