package space.dawdawich.controller.model.analyzer

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "strategy"
)
// Names must be consists with TradeStrategy enum
@JsonSubTypes(
    JsonSubTypes.Type(value = CreateGridAnalyzerRequest::class, name = "GRID_TABLE_STRATEGY"),
    JsonSubTypes.Type(value = CreateCandleTailAnalyzerRequest::class, name = "CANDLE_TAIL_STRATEGY"),
    JsonSubTypes.Type(value = CreateRSIGridAnalyzerRequest::class, name = "RSI_GRID_TABLE_STRATEGY"),
)
sealed class CreateAnalyzerRequest
