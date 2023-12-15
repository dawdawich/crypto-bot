package space.dawdawich.repositories.entity

import kotlinx.serialization.Serializable
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document("grid_table_analyzer")
@Serializable
data class GridTableAnalyzerDocument(
    @Id
    val id: String,
    @Indexed
    val accountId: String,
    val public: Boolean,
    val diapason: Int,
    val gridSize: Int,
    val multiplayer: Int,
    val positionStopLoss: Int,
    val positionTakeProfit: Int,
    val symbolInfo: SymbolInfoDocument,
    var startCapital: Double,
    var isActive: Boolean,
    var money: Double = startCapital,
    var middlePrice: Double? = null,
    val createTime: Long = System.currentTimeMillis(),
    var updateTime: Long = createTime
)
