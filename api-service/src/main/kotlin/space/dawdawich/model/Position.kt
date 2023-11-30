package space.dawdawich.model

data class Position(
    val entryPrice: Double,
    val size: Double,
    val closePrice: Double?,
    val isLong: Boolean,
    val createTime: Long,
    val closeTime: Long?
)
