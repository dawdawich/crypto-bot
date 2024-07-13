package space.dawdawich.controller.model.analyzer

import kotlinx.serialization.Serializable
import space.dawdawich.model.constants.Market

/**
 * Represents a request to create multiple analyzers in bulk.
 *
 * @property symbols The list of symbols to create analyzers for.
 * @property stopLossMin The minimum stop loss value.
 * @property stopLossMax The maximum stop loss value. Default is the same as stopLossMin.
 * @property stopLossStep The step size for the stop loss value.
 * @property takeProfitMin The minimum take profit value.
 * @property takeProfitMax The maximum take profit value. Default is the same as takeProfitMin.
 * @property takeProfitStep The step size for the take profit value.
 * @property diapasonMin The minimum diapason value.
 * @property diapasonMax The maximum diapason value. Default is the same as diapasonMin.
 * @property diapasonStep The step size for the diapason value.
 * @property gridSizeMin The minimum grid size value.
 * @property gridSizeMax The maximum grid size value. Default is the same as gridSizeMin.
 * @property gridSizeStep The step size for the grid size value.
 * @property multiplierMin The minimum multiplier value.
 * @property multiplierMax The maximum multiplier value. Default is the same as multiplierMin.
 * @property multiplierStep The step size for the multiplier value.
 * @property startCapital The starting capital value.
 * @property demoAccount Whether the analyzers are for a demo account or not.
 * @property market The market in which the analyzers are created.
 * @property active Whether the analyzers should be active or not.
 * @property public Whether the analyzers are public or not.
 * @property strategy The trade strategy for the analyzers.
 * @property folders The list of folders to assign the analyzers to.
 */
@Serializable
data class CreateGridAnalyzerBulkRequest(
    override val stopLossMin: Int,
    override val stopLossMax: Int = stopLossMin,
    override val stopLossStep: Int,
    override val takeProfitMin: Int,
    override val takeProfitMax: Int = takeProfitMin,
    override val takeProfitStep: Int,
    override val symbols: List<String>,
    val diapasonMin: Int,
    val diapasonMax: Int = diapasonMin,
    val diapasonStep: Int,
    val gridSizeMin: Int,
    val gridSizeMax: Int = gridSizeMin,
    val gridSizeStep: Int,
    val multiplierMin: Int,
    val multiplierMax: Int = multiplierMin,
    val multiplierStep: Int,
    override val startCapital: Int,
    override val demoAccount: Boolean,
    override val market: Market,
    override val active: Boolean,
    override val public: Boolean,
    override val folders: List<String>,
) : CreateAnalyzerBulkRequest()
