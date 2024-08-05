package space.dawdawich.model.strategy

class RSIGridStrategyConfigModel(
    override val id: String,
    override val symbol: String,
    override val money: Double,
    override val multiplier: Int,
    override val stopLoss: Int,
    override val takeProfit: Int,
    override val minQtyStep: Double,
    val gridSize: Int,
    override val kLineDuration: Int
) : KLineStrategyConfigModel()
