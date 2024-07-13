package space.dawdawich.controller.model.analyzer

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import space.dawdawich.model.constants.Market

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "strategy"
)
// Names must be consists with TradeStrategy enum
@JsonSubTypes(
    JsonSubTypes.Type(value = CreateGridAnalyzerBulkRequest::class, name = "GRID_TABLE_STRATEGY"),
    JsonSubTypes.Type(value = CreateCandleTailAnalyzerBulkRequest::class, name = "CANDLE_TAIL_STRATEGY"),
)
sealed class CreateAnalyzerBulkRequest {
    abstract val stopLossMin: Int
    abstract val stopLossMax: Int
    abstract val stopLossStep: Int
    abstract val takeProfitMin: Int
    abstract val takeProfitMax: Int
    abstract val takeProfitStep: Int
    abstract val symbols: List<String>
    abstract val startCapital: Int
    abstract val demoAccount: Boolean
    abstract val market: Market
    abstract val active: Boolean
    abstract val public: Boolean
    abstract val folders: List<String>
}
