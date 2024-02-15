package space.dawdawich.controller.model

import kotlinx.serialization.Serializable
import space.dawdawich.model.constants.AnalyzerChooseStrategy
import space.dawdawich.repositories.entity.constants.ManagerStatus

@Serializable
data class TradeManagerRequest(
    val apiTokenId: String,
    val status: ManagerStatus,
    val analyzerChooseStrategy: AnalyzerChooseStrategy,
    val customAnalyzerId: String,
    val stopLoss: Int? = null,
    val takeProfit: Int? = null,
)
