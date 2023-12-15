package space.dawdawich.configuration.provider.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountDetails(val accountId: String, val role: String)
