package space.dawdawich.repositories.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("grid_table_analyzer")
data class GridTableAnalyzerDocument(
    @Id
    val id: String,
    val diapason: Int,
    val gridSize: Int,
    val multiplayer: Int,
    val positionStopLoss: Int,
    val positionTakeProfit: Int,
    val symbolInfo: SymbolInfoDocument,
    var money: Double,
    var isActive: Boolean,
    var middlePrice: Double? = null
)
