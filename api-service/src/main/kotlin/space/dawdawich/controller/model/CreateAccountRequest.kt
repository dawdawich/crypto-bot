package space.dawdawich.controller.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountRequest(
    val username: String,
    val name: String,
    val surname: String,
    val email: String,
    val password: String,
)
