package space.dawdawich.model.strategy

class GridStrategyConfigModel(
    id: String,
    symbol: String,
    money: Double,
    multiplier: Int,
    val diapason: Int,
    val gridSize: Int,
    val stopLoss: Int,
    val takeProfit: Int,
    val priceMinStep: Double,
    val middlePrice: Double,
    val minPrice: Double,
    val maxPrice: Double,
    val step: Double,
    val pricesGrid: Set<Double>
) : StrategyConfigModel(id, symbol, money, multiplier)
