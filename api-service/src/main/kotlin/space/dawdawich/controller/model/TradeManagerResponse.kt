package space.dawdawich.controller.model

import space.dawdawich.repositories.entity.TradeManagerDocument

data class TradeManagerResponse(
    val id: String,
    val chooseStrategy: String,
    val customAnalyzerId: String,
    val active: Boolean,
    val apiTokenId: String,
    val createTime: Long,
    val updateTime: Long
) {
    companion object {
        fun TradeManagerDocument.convert() = TradeManagerResponse(this.id, this.chooseStrategy.name, this.customAnalyzerId, this.isActive, this.apiTokensId, this.createTime, this.updateTime)
    }
}
