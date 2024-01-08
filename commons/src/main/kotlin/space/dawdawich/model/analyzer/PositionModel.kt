package space.dawdawich.model.analyzer

import kotlinx.serialization.Serializable

@Serializable
data class PositionModel(
    val long: Boolean,
    val size: Double,
    val entryPrice: Double
)
