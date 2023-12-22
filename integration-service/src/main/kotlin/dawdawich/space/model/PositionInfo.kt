package dawdawich.space.model

data class PositionInfo(val symbol: String, val isLong: Boolean, var size: Double, var entryPrice: Double, val positionIdx: Int, var updateTime: Long)