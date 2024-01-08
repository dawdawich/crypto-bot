package space.dawdawich.model.manager

import kotlinx.serialization.Serializable
import space.dawdawich.model.analyzer.PositionModel

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
