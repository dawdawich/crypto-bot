package space.dawdawich.model.strategy

import kotlinx.serialization.Serializable
import space.dawdawich.model.analyzer.PositionModel

@Serializable
class GridTableStrategyRuntimeInfoModel(
    override val id: String,
    val prices: Set<Double>,
    override val currentPrice: Double,
    val middlePrice: Double,
    val minPrice: Double,
    val maxPrice: Double,
    val step: Double,
    override val position: PositionModel?
) : StrategyRuntimeInfoModel()
