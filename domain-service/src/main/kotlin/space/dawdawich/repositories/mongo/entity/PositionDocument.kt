package space.dawdawich.repositories.mongo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document(collection = "manager_positions")
data class PositionDocument(
    val entryPrice: Double,
    val closePrice: Double,
    val qty: Double,
    val symbol: String,
    val createTime: Long,
    val closeTime: Long,
    val trend: String,
    @Id
    val id: String = UUID.randomUUID().toString(),
    var isActionPosition: Boolean = false
)
