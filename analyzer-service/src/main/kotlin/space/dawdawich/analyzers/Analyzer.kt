package space.dawdawich.analyzers

import space.dawdawich.strategy.StrategyRunner
import space.dawdawich.utils.findLargestRange
import space.dawdawich.utils.findLowestRange
import java.util.*
import kotlin.properties.Delegates

class Analyzer(
    private val strategyRunner: StrategyRunner,
    currentPrice: Double,
    val symbol: String,
    val accountId: String,
    val id: String = UUID.randomUUID().toString()
) {
    private var snapshots : Queue<Double> = ArrayDeque()
    private val maxCountOfSnapshots : Int = 1440
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

    fun getRuntimeInfo() = strategyRunner.getRuntimeInfo()

    fun getStrategyConfig() = strategyRunner.getStrategyConfig()

    fun getMoney() = strategyRunner.money

    fun updateSnapshot() {
        if (snapshots.size >= maxCountOfSnapshots) {
            snapshots.poll()
        }
        snapshots.offer(strategyRunner.money)
    }

    fun calculateStabilityCoef(): Double {
        if (snapshots.isEmpty()) {
            stabilityCoef = 0.0
            return stabilityCoef
        }
        val listToProcess = snapshots.toList()

        val biggestDiapasonOfSnapshots = findLargestRange(listToProcess).size.toDouble()
        val smallestDiapasonOfSnapshots = findLowestRange(listToProcess).size.toDouble()

        stabilityCoef = biggestDiapasonOfSnapshots / smallestDiapasonOfSnapshots

        return if (stabilityCoef.isNaN()) {
            stabilityCoef = 0.0
            stabilityCoef
        } else {
            stabilityCoef
        }
    }

}
