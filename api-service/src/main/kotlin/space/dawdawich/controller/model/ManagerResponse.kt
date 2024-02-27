package space.dawdawich.controller.model

import kotlinx.serialization.Serializable
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.repositories.entity.constants.ManagerStatus

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
