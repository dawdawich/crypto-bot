package space.dawdawich.controller.model.manager

import kotlinx.serialization.Serializable
import space.dawdawich.repositories.mongo.entity.TradeManagerDocument
import space.dawdawich.repositories.constants.ManagerStatus

/**
 * Represents a response from the Manager class.
 *
 * @param id The ID of the manager.
 * @param customName The custom name for the manager.
 * @param status The status of the manager.
 * @param market The market of the manager.
 * @param analyzersCount The count of analyzers.
 * @param stopLoss The stop loss value for the manager.
 * @param takeProfit The take profit value for the manager.
 */
@Serializable
data class ManagerResponse(
    val id: String,
    val customName: String?,
    val status: ManagerStatus,
    val market: String,
    val analyzersCount: Int,
    val stopLoss: Int? = null,
    val takeProfit: Int? = null,
) {
    companion object {
        fun TradeManagerDocument.convert() = ManagerResponse(this.id, this.chooseStrategy.name, this.status, this.apiTokenId, -1)
    }
}
