package space.dawdawich.model.strategy

import kotlinx.serialization.Serializable

/**
 * Represents the configuration model for Grid Strategy. Uses as DTO object between different services.
 *
 * @property id The identifier of the strategy.
 * @property symbol The symbol of the trading pair.
 * @property money The amount of money available for trading.
 * @property multiplier The margin multiplier.
 * @property diapason The size of the diapason.
 * @property gridSize The number of orders to be placed on each side of the current price.
 * @property stopLoss The stop loss value.
 * @property takeProfit The take profit value.
 * @property priceMinStep The minimum price step.
 * @property minQtyStep The minimum quantity step.
 * @property middlePrice The middle price of the diapason.
 * @property minPrice The minimum price of the diapason.
 * @property maxPrice The maximum price of the diapason.
 * @property step The step value for order prices.
 * @property pricesGrid The set of order prices.
 */
@Serializable
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
