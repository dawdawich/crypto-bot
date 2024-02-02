package space.dawdawich.model.strategy.runtimeModel

import kotlinx.serialization.Serializable
import space.dawdawich.model.analyzer.PositionModel

@Serializable
class SwitchStrategyRuntimeInfoModel(
    override val id: String,
    override val currentPrice: Double,
    override val position: PositionModel?
) : StrategyRuntimeInfoModel()
