package space.dawdawich.analyzers

import space.dawdawich.model.constants.Market
import space.dawdawich.strategy.PriceChangeStrategyRunner
import space.dawdawich.strategy.StrategyRunner
import space.dawdawich.utils.findLargestRange
import space.dawdawich.utils.findLowestRange
import java.util.*
import kotlin.properties.Delegates

abstract class Analyzer(
    protected val strategyRunner: StrategyRunner,
    currentPrice: Double,
    val startCapital: Double,
    val symbol: String,
    val accountId: String,
    val market: Market,
    val demoAccount: Boolean,
    val id: String = UUID.randomUUID().toString()
) {
    var previousSnapshotMoney : Double = -1.0
    var readyToUpdateStability = false
    var stabilityCoef : Double = 0.0
        private set

    private var currentPrice: Double by Delegates.observable(currentPrice) { _, oldPrice, newPrice ->
        if (oldPrice > 0 && newPrice > 0) {
            strategyRunner.acceptPriceChange(oldPrice, newPrice)
        }
    }

    fun acceptPriceChange(currentPrice: Double) {
        this.currentPrice = currentPrice
    }

    fun getMoney() = strategyRunner.moneyWithProfit

    fun calculateStabilityCoef(listOfMoneySnapshots: List<Double>): Double {
        if (listOfMoneySnapshots.isEmpty()) {
            stabilityCoef = 0.0
            return stabilityCoef
        }

        val biggestDiapasonOfSnapshots = findLargestRange(listOfMoneySnapshots).size.toDouble()
        val smallestDiapasonOfSnapshots = findLowestRange(listOfMoneySnapshots).size.toDouble()

        stabilityCoef = biggestDiapasonOfSnapshots / smallestDiapasonOfSnapshots
        readyToUpdateStability = false

        return if (stabilityCoef.isNaN()) {
            stabilityCoef = 0.0
            stabilityCoef
        } else {
            stabilityCoef
        }
    }

    fun getStrategyConfig() = strategyRunner.getStrategyConfig()

    fun getRuntimeInfo() = (strategyRunner as PriceChangeStrategyRunner).getRuntimeInfo()
}
