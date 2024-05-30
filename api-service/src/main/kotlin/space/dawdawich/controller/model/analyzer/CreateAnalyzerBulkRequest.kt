package space.dawdawich.controller.model.analyzer

import kotlinx.serialization.Serializable
import space.dawdawich.model.constants.Market
import space.dawdawich.model.constants.TradeStrategy

@Serializable
data class CreateAnalyzerBulkRequest(
    val symbols: List<String>,
    val stopLossMin: Int,
    val stopLossMax: Int = stopLossMin,
    val stopLossStep: Int,
    val takeProfitMin: Int,
    val takeProfitMax: Int = takeProfitMin,
    val takeProfitStep: Int,
    val diapasonMin: Int,
    val diapasonMax: Int = diapasonMin,
    val diapasonStep: Int,
    val gridSizeMin: Int,
    val gridSizeMax: Int = gridSizeMin,
    val gridSizeStep: Int,
    val multiplierMin: Int,
    val multiplierMax: Int = multiplierMin,
    val multiplierStep: Int,
    val startCapital: Int,
    val demoAccount: Boolean,
    val market: Market,
    val active: Boolean,
    val public: Boolean,
    val strategy: TradeStrategy,
    val folders: List<String>,
) {
    fun calculateSize() =
        symbols.size *
                ((stopLossMax - stopLossMin) / stopLossStep) *
                ((takeProfitMax - takeProfitMin) / takeProfitStep) *
                ((diapasonMax - diapasonMin) / diapasonStep) *
                ((gridSizeMax - gridSizeMin) / gridSizeStep) *
                ((multiplierMax - multiplierMin) / multiplierStep)

}
