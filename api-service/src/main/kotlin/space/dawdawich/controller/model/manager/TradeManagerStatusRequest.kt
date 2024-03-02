package space.dawdawich.controller.model.manager

import kotlinx.serialization.Serializable
import space.dawdawich.repositories.constants.ManagerStatus

@Serializable
data class TradeManagerStatusRequest(val status: ManagerStatus)
