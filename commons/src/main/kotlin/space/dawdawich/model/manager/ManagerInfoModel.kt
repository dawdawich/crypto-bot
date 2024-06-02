package space.dawdawich.model.manager

import kotlinx.serialization.Serializable
import space.dawdawich.model.analyzer.PositionModel

/**
 * Represents the information of a manager. Uses as DTO object between different services.
 *
 * @property id The unique identifier of the manager.
 * @property currentPrice The current price of the manager.
 * @property analyzerId The identifier of the analyzer used by the manager.
 * @property startCapital The initial capital of the manager.
 * @property currentCapital The current capital of the manager.
 * @property orders The list of orders made by the manager.
 * @property positions The list of positions held by the manager.
 */
@Serializable
data class ManagerInfoModel(
    val id: String,
    val currentPrice: Double,
    val analyzerId: String,
    val startCapital: Double,
    val currentCapital: Double,
    val orders: List<String>,
    val positions: List<PositionModel>
)
