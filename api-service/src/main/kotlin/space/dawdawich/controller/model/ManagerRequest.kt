package space.dawdawich.controller.model

import kotlinx.serialization.Serializable
import space.dawdawich.model.constants.AnalyzerChooseStrategy
import space.dawdawich.repositories.constants.ManagerStatus

@Serializable
data class ManagerRequest(
    val apiTokenId: String,
    val customName: String?,
    val status: ManagerStatus,
    val analyzerChooseStrategy: AnalyzerChooseStrategy,
    val refreshAnalyzerTime: Int,
    val folder: String = "ALL",
    val stopLoss: Int? = null,
    val takeProfit: Int? = null,
)
