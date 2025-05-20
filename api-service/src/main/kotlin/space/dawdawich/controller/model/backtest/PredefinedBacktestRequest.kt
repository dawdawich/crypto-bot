package space.dawdawich.controller.model.backtest

import kotlinx.serialization.Serializable

@Serializable
data class PredefinedBacktestRequest(
    val symbols: List<String>,
    val startCapital: Double,
)
