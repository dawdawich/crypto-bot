package space.dawdawich.controller.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountResponse(val id: String, val username: String, val name: String, val surname: String, val email: String, val createTime: Long, val tokens: List<ApiTokenResponse>)
