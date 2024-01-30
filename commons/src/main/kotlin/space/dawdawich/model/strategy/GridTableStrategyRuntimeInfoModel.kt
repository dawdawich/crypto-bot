package space.dawdawich.model.strategy

import space.dawdawich.model.analyzer.PositionModel

class GridTableStrategyRuntimeInfoModel(
    id: String,
    val prices: Set<Double>,
    currentPrice: Double,
    val middlePrice: Double,
    val minPrice: Double,
    val maxPrice: Double,
    val step: Double,
    position: PositionModel?
) : StrategyRuntimeInfoModel(id, currentPrice, position)
