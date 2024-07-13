package space.dawdawich.model.strategy

import kotlinx.serialization.Serializable

/**
 * Represents the runtime information of an analyzer. Uses as DTO object between different services.
 *
 * @property money The amount of money.
 * @property stability The stability coefficient.
 * @property positionDirection The direction of the position.
 * @property positionEntryPrice The entry price of the position.
 * @property positionSize The size of the position.
 */
@Serializable
data class AnalyzerRuntimeInfoModel(
    val money: Double,
    val stability: Double?,
    val positionDirection: String? = null,
    val positionEntryPrice: Double? = null,
    val positionSize: Double? = null,
) : java.io.Serializable
