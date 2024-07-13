package space.dawdawich.controller.model.analyzer

import kotlinx.serialization.Serializable
import space.dawdawich.model.constants.Market
import space.dawdawich.model.constants.TradeStrategy

/**
 * Data class representing a request to create an analyzer.
 *
 * @property public Indicates if the analyzer is public.
 * @property diapason The diapason value.
 * @property gridSize The grid size value.
 * @property multiplier The multiplier value.
 * @property stopLoss The stop loss value.
 * @property takeProfit The take profit value.
 * @property symbol The symbol for analysis/trades.
 * @property startCapital The starting capital.
 * @property active Indicates if the analyzer is active.
 * @property market The market in which analysis/trades can be made.
 * @property demoAccount Indicates if the analyzer is a demo account.
 * @property folders The list of folders to which the analyzer belongs.
 */
@Serializable
data class CreateGridAnalyzerRequest(
    val public: Boolean,
    val diapason: Int,
    val gridSize: Int,
    val multiplier: Int,
    val stopLoss: Int,
    val takeProfit: Int,
    val symbol: String,
    val startCapital: Double,
    val active: Boolean,
    val market: Market,
    val demoAccount: Boolean,
    val folders: List<String>,
) : CreateAnalyzerRequest()
