package space.dawdawich.model

import space.dawdawich.model.constants.AnalyzerChooseStrategy
import space.dawdawich.model.constants.Market

/**
 * Represents a RequestProfitableAnalyzer that is used for analyzing profitable requests. Uses as DTO object between different services.
 *
 * @property accountId The ID of the account used for analysis.
 * @property chooseStrategy The strategy used for selecting the analyzer's search algorithm.
 * @property currentAnalyzerId The ID of the current analyzer being used.
 * @property managerMoney The amount of money managed by the analyzer.
 * @property demoAccount Indicates if the account is a demo account.
 * @property market The market in which the analysis/trades can be made.
 */
data class RequestProfitableAnalyzer(
        val accountId: String,
        val chooseStrategy: AnalyzerChooseStrategy,
        val currentAnalyzerId: String? = null,
        val managerMoney: Double,
        val demoAccount: Boolean,
        val market: Market
) : java.io.Serializable
