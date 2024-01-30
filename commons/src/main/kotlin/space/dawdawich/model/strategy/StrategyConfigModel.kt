package space.dawdawich.model.strategy

open class StrategyConfigModel(
    val id: String,
    val symbol: String,
    var money: Double,
    val multiplier: Int,
    val priceMinStep: Double,
    val minQtyStep: Double
)
