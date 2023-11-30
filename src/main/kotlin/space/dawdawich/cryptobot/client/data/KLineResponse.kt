package space.dawdawich.cryptobot.client.data

import kotlinx.serialization.Serializable

@Serializable
data class KLineResponse(val type: String, val topic: String, val data: List<KLineData>, val ts: Long)

@Serializable
data class KLineData(val start: Long, val end: Long, val interval: String, val open: String, val close: String, val high: String, val low: String, val confirm: Boolean, val timestamp: Long)
