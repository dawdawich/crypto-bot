package space.dawdawich.controller.model.manager

import kotlinx.serialization.Serializable
import space.dawdawich.model.constants.AnalyzerChooseStrategy
import space.dawdawich.repositories.constants.ManagerStatus

/**
 * Represents a Manager Request.
 *
 * @property apiTokenId The ID of the API token.
 * @property customName The custom name for the manager (optional).
 * @property status The status of the manager.
 * @property analyzerChooseStrategy The strategy for choosing an analyzer's search algorithm.
 * @property refreshAnalyzerTime The refresh time for the analyzer.
 * @property folder The folder for the manager (default value is "ALL").
 * @property stopLoss The stop loss value for the manager (optional).
 * @property takeProfit The take profit value for the manager (optional).
 */
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
