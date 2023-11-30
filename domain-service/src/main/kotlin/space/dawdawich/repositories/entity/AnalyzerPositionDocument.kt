package space.dawdawich.repositories.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document("analyzer_positions")
data class AnalyzerPositionDocument(
    @Id
    val id: String,
    val analyzerId: String,
    val positionEntryPrice: Double,
    val positionSize: Double,
    val isLong: Boolean,
    val closePrice: Double?,
    val closeTime: Long?,
    val createTime: Long = System.currentTimeMillis(),
)
