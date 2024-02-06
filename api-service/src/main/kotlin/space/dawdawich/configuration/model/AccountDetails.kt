package space.dawdawich.configuration.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountDetails(val accountId: String, val role: String)
