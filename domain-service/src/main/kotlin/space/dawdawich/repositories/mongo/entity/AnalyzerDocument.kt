package space.dawdawich.repositories.mongo.entity

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import space.dawdawich.model.constants.Market

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = GridTableAnalyzerDocument::class, name = "grid-table-strategy"),
    JsonSubTypes.Type(value = CandleTailStrategyAnalyzerDocument::class, name = "candle-tail-strategy"),
)
@Document(collection = "analyzer")
sealed class AnalyzerDocument(
    @Id
    open val id: String,
    @Indexed
    open val accountId: String,
    open val public: Boolean,
    open val multiplier: Int,
    open val positionStopLoss: Int,
    open val positionTakeProfit: Int,
    open val symbolInfo: SymbolInfoDocument,
    open var startCapital: Double,
    open var isActive: Boolean,
    open val demoAccount: Boolean,
    open val market: Market,
    open var money: Double = startCapital,
    open var stabilityCoef: Double? = null,
    open val pNl1: Int? = null,
    open val pNl12: Int? = null,
    open val pNl24: Int? = null,
    open val createTime: Long = System.currentTimeMillis(),
    open var updateTime: Long = createTime
)
