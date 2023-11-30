package space.dawdawich.cryptobot.service.response

import kotlinx.serialization.Serializable

@Serializable
data class PositionData(val positionIdx: Int, val size: String, val side: String)
