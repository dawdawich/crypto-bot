package space.dawdawich.controller.model

import kotlinx.serialization.Serializable

@Serializable
data class SymbolResponse(val symbol: String, val partition: Int, val isOneWayMode: Boolean, val priceMinStep: Double)
