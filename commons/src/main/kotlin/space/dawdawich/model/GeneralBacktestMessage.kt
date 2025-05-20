package space.dawdawich.model

import kotlinx.serialization.Serializable

@Serializable
data class GeneralBacktestMessage(
    val requestId: String,
    val symbols: List<String>,
    val startCapital: Double
) : java.io.Serializable
