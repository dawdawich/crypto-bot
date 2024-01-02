package space.dawdawich.repositories.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import space.dawdawich.repositories.entity.constants.AnalyzerChooseStrategy
import space.dawdawich.repositories.entity.constants.ManagerStatus

@Document("trade_manager")
data class TradeManagerDocument(
    @Id
    val id: String,
    val accountId: String,
    var apiTokensId: String,
    var money: Double = 0.0,
    var chooseStrategy: AnalyzerChooseStrategy = AnalyzerChooseStrategy.BIGGEST_BY_MONEY,
    var customAnalyzerId: String = "",
    var status: ManagerStatus = ManagerStatus.INACTIVE,
    val stopLoss: Int? = null,
    val takeProfit: Int? = null,
    var stopDescription: String? = null,
    var errorDescription: String? = null,
    val createTime: Long = System.currentTimeMillis(),
    var updateTime: Long = System.currentTimeMillis()
)
