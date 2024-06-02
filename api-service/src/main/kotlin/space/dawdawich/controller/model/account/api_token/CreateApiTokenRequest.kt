package space.dawdawich.controller.model.account.api_token

import kotlinx.serialization.Serializable

/**
 * Represents a request to create an API token.
 *
 * @property market The market for which the token will be generated.
 * @property apiKey The API key associated with the token.
 * @property secretKey The secret key associated with the token.
 * @property demo Indicates whether the token is for a demo account or not.
 */
@Serializable
data class CreateApiTokenRequest(val market: String, val apiKey: String, val secretKey: String, val demo: Boolean)
