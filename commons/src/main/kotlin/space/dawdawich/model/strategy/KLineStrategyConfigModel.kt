package space.dawdawich.model.strategy

import kotlinx.serialization.Serializable

@Serializable
sealed class KLineStrategyConfigModel : StrategyConfigModel(), java.io.Serializable {
    abstract val kLineDuration: Int
}
