package space.dawdawich.model.strategy.configModel

class SwitchStrategyConfigModel(
    override val id: String,
    override val symbol: String,
    override val money: Double,
    override val multiplier: Int,
    override val priceMinStep: Double,
    override val minQtyStep: Double,
    val capitalOrderPerPercent: Int,
    val switchCounterValue: Int,
    val coefficientBetweenOrders: Double,
) : StrategyConfigModel()
