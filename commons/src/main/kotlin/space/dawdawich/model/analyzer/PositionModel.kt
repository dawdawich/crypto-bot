package space.dawdawich.model.analyzer

import kotlinx.serialization.Serializable

/**
 * Represents a position in a trading strategy. Uses as DTO object.
 *
 * @property long Specifies if the position is long (true) or short (false).
 * @property size The size of the position.
 * @property entryPrice The entry price of the position.
 */
@Serializable
data class PositionModel(
    val long: Boolean,
    val size: Double,
    val entryPrice: Double
) : java.io.Serializable
