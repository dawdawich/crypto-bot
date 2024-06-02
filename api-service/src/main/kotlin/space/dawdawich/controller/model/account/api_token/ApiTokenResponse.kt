package space.dawdawich.controller.model.account.api_token

import kotlinx.serialization.Serializable

/**
 * Represents the response for an API token.
 *
 * @property id The unique ID of the token.
 * @property apiKey The API key associated with the token.
 * @property market The market name for which the token is generated.
 * @property test Whether the token is for a test account or not.
 */
@Serializable
data class ApiTokenResponse(val id: String, val apiKey: String, val market: String, val test: Boolean)
