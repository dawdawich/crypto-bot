package space.dawdawich.model.strategy

import kotlinx.serialization.Serializable
import space.dawdawich.model.analyzer.PositionModel

/**
 * Represents the runtime information model for Grid Table Strategy. Uses as DTO object between different services.
 *
 * @property id The identifier of the strategy.
 * @property prices The set of order prices.
 * @property currentPrice The current price of the diapason.
 * @property middlePrice The middle price of the diapason.
 * @property minPrice The minimum price of the diapason.
 * @property maxPrice The maximum price of the diapason.
 * @property step The step value for order prices.
 * @property position The position information related to the strategy.
 */
@Serializable
class GridTableStrategyRuntimeInfoModel(
    override val id: String,
    val prices: Set<Double>,
    val currentPrice: Double,
    val middlePrice: Double,
    val minPrice: Double,
    val maxPrice: Double,
    val step: Double,
    override val position: PositionModel?
) : StrategyRuntimeInfoModel(), java.io.Serializable
