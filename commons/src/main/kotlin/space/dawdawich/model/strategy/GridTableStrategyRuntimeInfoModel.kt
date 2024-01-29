package space.dawdawich.model.strategy

import space.dawdawich.model.analyzer.PositionModel

class GridTableStrategyRuntimeInfoModel(
    id: String,
    val orders: List<String>,
    currentPrice: Double,
    val middlePrice: Double,
    position: PositionModel?
) : StrategyRuntimeInfoModel(id, currentPrice, position)
