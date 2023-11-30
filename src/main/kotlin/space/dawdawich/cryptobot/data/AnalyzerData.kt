package space.dawdawich.cryptobot.data

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzerData(val wallet: Double, val stopLoss: Float, val takeProfit: Float, val switcherTicks: Int, val multiplier: Int, val pair: String)
