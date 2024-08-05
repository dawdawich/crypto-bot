package space.dawdawich.strategy.model

data class KLine(val openPrice: Double, val closePrice: Double, val highPrice: Double, val lowPrice: Double, val rsi: Double = Double.NaN)
