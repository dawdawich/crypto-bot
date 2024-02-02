package space.dawdawich.model.strategy

class GridStrategyConfigModel(
    override val id: String,
    override val symbol: String,
    override val money: Double,
    override val multiplier: Int,
    val diapason: Int,
    val gridSize: Int,
    val stopLoss: Int,
    val takeProfit: Int,
    override val priceMinStep: Double,
    override val minQtyStep: Double,
    val middlePrice: Double,
    val minPrice: Double,
    val maxPrice: Double,
    val step: Double,
    val pricesGrid: Set<Double>
) : StrategyConfigModel()
