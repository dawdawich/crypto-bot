package space.dawdawich.controller.model.account

import kotlinx.serialization.Serializable
import space.dawdawich.controller.model.account.api_token.ApiTokenResponse

@Serializable
data class AccountResponse(val id: String, val username: String, val name: String, val surname: String, val email: String, val createTime: Long, val tokens: List<ApiTokenResponse>)
