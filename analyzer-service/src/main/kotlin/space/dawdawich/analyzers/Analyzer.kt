package space.dawdawich.analyzers

import space.dawdawich.strategy.StrategyRunner
import java.util.*
import kotlin.properties.Delegates

class Analyzer(
    private val strategyRunner: StrategyRunner,
    currentPrice: Double,
    val symbol: String,
    val accountId: String,
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

    fun getMoney() = strategyRunner.money
}
