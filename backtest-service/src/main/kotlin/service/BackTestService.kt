package space.dawdawich.service

import kotlinx.coroutines.*
import org.springframework.stereotype.Service
import space.dawdawich.common.TpAndSlChecker
import space.dawdawich.common.TpAndSlChecker.CheckResult.SL
import space.dawdawich.common.TpAndSlChecker.CheckResult.TP
import space.dawdawich.model.BackTestConfiguration
import space.dawdawich.model.BackTestResult
import space.dawdawich.repositories.mongo.PriceTickRepository
import space.dawdawich.strategy.strategies.GridTableStrategyRunner

@Service
class BackTestService(private val priceTickRepository: PriceTickRepository) {

    fun processConfigs(runConfigurations: List<BackTestConfiguration>, startTime: Long): List<BackTestResult> {
        val symbolHashCodes =
            runConfigurations
                .map { config -> config.symbol.symbol.hashCode() }
                .distinct()
                .associateWith {
                    priceTickRepository.findAllByTimeIsGreaterThanAndPair(startTime, it)
                }

        return runBlocking {
            runConfigurations.map { config ->
                async(Dispatchers.Default) {
                    // do the actual backTest in parallel on Default dispatcher
                    val prices = symbolHashCodes[config.symbol.symbol.hashCode()]!!
                    val result = backTest(config, prices.map { it.price })
                    BackTestResult(config, prices.first().time, prices.last().time, result)
                }
            }.awaitAll()
        }
    }

    fun processConfig(runConfiguration: BackTestConfiguration, startTime: Long): BackTestResult = priceTickRepository
        .findAllByTimeIsGreaterThanAndPair(startTime, runConfiguration.symbol.symbol.hashCode())
        .let { prices ->
            BackTestResult(
                runConfiguration,
                prices.first().time,
                prices.last().time,
                backTest(runConfiguration, prices.map { it.price })
            )
        }

    private fun backTest(
        runConfiguration: BackTestConfiguration,
        prices: List<Double>,
    ): Double {
        val initialPrice = prices[0]

        fun createStrategyRunner(money: Double, initialPrice: Double) = GridTableStrategyRunner(
            initialPrice,
            money,
            runConfiguration.multiplier,
            runConfiguration.symbol.symbol,
            runConfiguration.diapason,
            runConfiguration.gridSize,
            runConfiguration.symbol.minPrice,
            runConfiguration.symbol.minOrderQty
        )


        var strategyRunner = createStrategyRunner(runConfiguration.startCapital, initialPrice)
        var checker =
            TpAndSlChecker(runConfiguration.startCapital, runConfiguration.takeProfit, runConfiguration.stopLoss)
        var skip = false
        var newMoney = runConfiguration.startCapital

        for (price in prices) {
            if (!skip) {
                skip = true
                continue
            }

            strategyRunner.acceptPriceChange(price)

            val pnl = strategyRunner.getPnL()
            val checkResult = checker.checkPnLExceedingBoundsWithSlUpdating(pnl)

            if (checkResult == TP || checkResult == SL) {
                newMoney += pnl
                strategyRunner = createStrategyRunner(newMoney, price)
                checker = TpAndSlChecker(newMoney, runConfiguration.takeProfit, runConfiguration.stopLoss)
            }
        }

        return newMoney + strategyRunner.getPnL()
    }
}
