package space.dawdawich.controller.model

import kotlinx.serialization.Serializable
import space.dawdawich.repositories.entity.TradeManagerDocument

@Serializable
data class TradeManagerResponse(
    val id: String,
    val chooseStrategy: String,
    val customAnalyzerId: String,
    val status: String,
    val apiTokenId: String,
    val createTime: Long,
    val updateTime: Long,
    val errorDescription: String? = null
) {
    companion object {
        fun TradeManagerDocument.convert() = TradeManagerResponse(this.id, this.chooseStrategy.name, this.customAnalyzerId, this.status.name, this.apiTokensId, this.createTime, this.updateTime, this.errorDescription)
    }
}
