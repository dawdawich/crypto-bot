package dawdawich.space.model

import kotlinx.serialization.Serializable

@Serializable
data class PositionInfo(val symbol: String, val isLong: Boolean, var size: Double, var entryPrice: Double, val positionIdx: Int, var updateTime: Long)
