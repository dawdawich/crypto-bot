package space.dawdawich.repositories.mongo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import space.dawdawich.model.constants.Market

@Document("grid_table_analyzer")
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
    val demoAccount: Boolean,
    val market: Market,
    var money: Double = startCapital,
    var middlePrice: Double? = null,
    var stabilityCoef: Double? = null,
    val createTime: Long = System.currentTimeMillis(),
    var updateTime: Long = createTime
)
