package space.dawdawich.model.strategy

import kotlinx.serialization.Serializable
import space.dawdawich.model.analyzer.PositionModel

@Serializable
class RSIGridTableStrategyRuntimeInfoModel(
    override val id: String,
    val rsiValues: List<Double>,
    val currentPrice: Double,
    val step: Double,
    override val position: PositionModel?
) : StrategyRuntimeInfoModel(), java.io.Serializable
