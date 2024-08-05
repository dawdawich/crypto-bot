package space.dawdawich.model.analyzer

import kotlinx.serialization.Serializable

@Serializable
data class KLineRecord(val interval: String, val open: Double, val close: Double, val high: Double, val low: Double, var rsi: Double = Double.NaN)
