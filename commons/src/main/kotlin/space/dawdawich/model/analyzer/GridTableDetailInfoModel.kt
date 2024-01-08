package space.dawdawich.model.analyzer

import kotlinx.serialization.Serializable

@Serializable
data class GridTableDetailInfoModel(
    val id: String,
    val orders: List<String>,
    val currentPrice: Double,
    val positions: List<PositionModel>
)
