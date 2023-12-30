package space.dawdawich.controller.model

import kotlinx.serialization.Serializable
import space.dawdawich.repositories.entity.constants.ManagerStatus

@Serializable
data class TradeManagerStatusRequest(val status: ManagerStatus)
