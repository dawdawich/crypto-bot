package space.dawdawich.controller.model.manager

import kotlinx.serialization.Serializable
import space.dawdawich.repositories.constants.ManagerStatus

/**
 * Represents a request to change the status of a trade manager.
 *
 * @property status The status to set for the trade manager.
 */
@Serializable
data class TradeManagerStatusRequest(val status: ManagerStatus)
