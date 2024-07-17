package space.dawdawich.model.strategy

import kotlinx.serialization.Serializable
import space.dawdawich.model.analyzer.PositionModel

@Serializable
class CandleTailStrategyRuntimeInfoModel(
    override val id: String,
    override val position: PositionModel?
) : StrategyRuntimeInfoModel(), java.io.Serializable
