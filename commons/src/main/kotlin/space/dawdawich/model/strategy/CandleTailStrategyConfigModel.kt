package space.dawdawich.model.strategy

import kotlinx.serialization.Serializable

@Serializable
class CandleTailStrategyConfigModel(
    override val id: String,
    override val symbol: String,
    override val money: Double,
    override val multiplier: Int,
    override val stopLoss: Int,
    override val takeProfit: Int,
    override val kLineDuration: Int,
    override val minQtyStep: Double,
) : KLineStrategyConfigModel(), java.io.Serializable
