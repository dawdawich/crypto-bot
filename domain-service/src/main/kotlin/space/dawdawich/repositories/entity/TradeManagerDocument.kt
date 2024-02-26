package space.dawdawich.repositories.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import space.dawdawich.model.constants.AnalyzerChooseStrategy
import space.dawdawich.repositories.entity.constants.ManagerStatus

@Document("trade_manager")
data class TradeManagerDocument(
    @Id
    val id: String,
    val accountId: String,
    val customName: String?,
    var apiTokenId: String,
    var chooseStrategy: AnalyzerChooseStrategy,
    var folder: String = "ALL",
    var refreshAnalyzerMinutes: Int = 30,
    var status: ManagerStatus = ManagerStatus.INACTIVE,
    val stopLoss: Int? = null,
    val takeProfit: Int? = null,
    var stopDescription: String? = null,
    var errorDescription: String? = null,
    val createTime: Long = System.currentTimeMillis(),
    var updateTime: Long = System.currentTimeMillis()
)
