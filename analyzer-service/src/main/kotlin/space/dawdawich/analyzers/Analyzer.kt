package space.dawdawich.analyzers

import space.dawdawich.strategy.StrategyRunner
import java.util.*
import kotlin.properties.Delegates

class Analyzer<T : StrategyRunner>(
    private val strategyRunner: T,
    currentPrice: Double,
    val symbol: String,
    val id: String = UUID.randomUUID().toString()
) {
    private var currentPrice: Double by Delegates.observable(currentPrice) { _, oldPrice, newPrice ->
        if (oldPrice > 0 && newPrice > 0) {
            strategyRunner.acceptPriceChange(oldPrice, newPrice)
        }
    }

    fun acceptPriceChange(currentPrice: Double) {
        this.currentPrice = currentPrice
    }

    fun getRuntimeInfo() = strategyRunner.getRuntimeInfo()

    fun getStrategyConfig() = strategyRunner.getStrategyConfig()
}
