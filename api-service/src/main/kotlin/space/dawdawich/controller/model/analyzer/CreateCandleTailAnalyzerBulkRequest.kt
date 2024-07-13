package space.dawdawich.controller.model.analyzer

import kotlinx.serialization.Serializable
import space.dawdawich.model.constants.Market

@Serializable
data class CreateCandleTailAnalyzerBulkRequest(
    override val symbols: List<String>,
    override val stopLossMin: Int,
    override val stopLossMax: Int = stopLossMin,
    override val stopLossStep: Int,
    override val takeProfitMin: Int,
    override val takeProfitMax: Int = takeProfitMin,
    override val takeProfitStep: Int,
    val multiplierMin: Int,
    val multiplierMax: Int = multiplierMin,
    val multiplierStep: Int,
    val klineDurations: List<Int>,
    override val startCapital: Int,
    override val demoAccount: Boolean,
    override val market: Market,
    override val active: Boolean,
    override val public: Boolean,
    override val folders: List<String>,
) : CreateAnalyzerBulkRequest()
