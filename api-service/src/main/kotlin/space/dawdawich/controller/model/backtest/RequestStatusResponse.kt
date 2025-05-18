package space.dawdawich.controller.model.backtest

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

@Serializable
data class RequestStatusResponse(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("accountId")
    val accountId: String,
    @JsonProperty("status")
    val status: String
)
