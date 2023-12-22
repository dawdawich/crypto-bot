package space.dawdawich.controller.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateApiTokenRequest(val market: String, val apiKey: String, val secretKey: String, val test: Boolean)
