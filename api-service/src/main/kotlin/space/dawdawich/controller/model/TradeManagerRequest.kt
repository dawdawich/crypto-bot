package space.dawdawich.controller.model

import kotlinx.serialization.Serializable
import space.dawdawich.repositories.entity.constants.ManagerStatus

@Serializable
data class TradeManagerRequest(val apiTokenId: String, val status: ManagerStatus, val customAnalyzerId: String)
