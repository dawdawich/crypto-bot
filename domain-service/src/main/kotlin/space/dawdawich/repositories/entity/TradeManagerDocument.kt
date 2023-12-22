package space.dawdawich.repositories.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document("trade_manager")
data class TradeManagerDocument(
    @Id
    val id: String,
    val accountId: String,
    var apiTokensId: String,
    var money: Double = 0.0,
    var chooseStrategy: AnalyzerChooseStrategy = AnalyzerChooseStrategy.BIGGEST_BY_MONEY,
    var customAnalyzerId: String = "",
    var isActive: Boolean = false,
    val createTime: Long = System.currentTimeMillis(),
    var updateTime: Long = System.currentTimeMillis()
)

enum class AnalyzerChooseStrategy {
    BIGGEST_BY_MONEY, CUSTOM
}
