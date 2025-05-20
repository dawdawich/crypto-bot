package space.dawdawich.service

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.springframework.stereotype.Service
import space.dawdawich.common.TpAndSlChecker
import space.dawdawich.common.TpAndSlChecker.CheckResult.SL
import space.dawdawich.common.TpAndSlChecker.CheckResult.TP
import space.dawdawich.model.BackTestConfiguration
import space.dawdawich.model.BackTestResult
import space.dawdawich.repositories.mongo.PriceTickRepository
import space.dawdawich.strategy.strategies.GridTableStrategyRunner
import java.util.concurrent.Executors
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@Service
class BackTestService(private val priceTickRepository: PriceTickRepository) {

    val dispatcher = Executors
        .newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        .asCoroutineDispatcher()
    val log = KotlinLogging.logger {}

    @OptIn(ExperimentalAtomicApi::class)
    fun processConfigs(runConfigurations: List<BackTestConfiguration>, startTime: Long): List<BackTestResult> {
        val symbolHashCodes =
            runConfigurations
                .map { config -> config.symbol.symbol.hashCode() }
                .distinct()
                .associateWith {
                    priceTickRepository.findAllByTimeIsGreaterThanAndPair(startTime, it)
                }

        val size = runConfigurations.size
        val complete = AtomicInt(0)
        return runBlocking {
            runConfigurations
                .filter { config -> symbolHashCodes[config.symbol.symbol.hashCode()]!!.isNotEmpty() }
                .map { config ->
                    async(dispatcher) {
                        // do the actual backTest in parallel on Default dispatcher
                        val prices = symbolHashCodes[config.symbol.symbol.hashCode()]!!
                        val result = backTest(config, prices.sortedBy { it.time }.map { it.price })
                        log.info { "Processed ${complete.incrementAndFetch()} / $size" }
                        BackTestResult(config, prices.first().time, prices.last().time, result)
                    }
                }.awaitAll()
        }
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
//
//fun main() {
//    val pricesJson = Files.readString(Path.of("eth_prices.json"))
//
//    val prices = ObjectMapper().readValue(pricesJson, object : TypeReference<List<Map<String, Any>>>() {})
//        .map { map ->
//            map["time"] as Map<String, Any> to map["price"].toString().toDouble()
//        }
//        .map { pair ->
//            val time =
//                ((pair.first["high"] as Int).toLong() shl 32) or (((pair.first["low"] as Int).toLong() and 0xFFFFFFFFL))
//
//            time to pair.second
//        }.sortedBy { it.first }
//        .map { it.second }
//
//    val result =
//        backTest(
//            BackTestConfiguration(
//                SymbolDocument("ETHUSDT", 0.01, 199999.98, 0.01, 7240.0, 100.0, 0.01, 0.01),
//                1000.0,
//                10.0,
//                7,
//                50,
//                30,
//                20
//            ), prices
//        )
//
//    println(result)
//}
//
//fun backTest(
//    runConfiguration: BackTestConfiguration,
//    prices: List<Double>,
//): Double {
//    val initialPrice = prices[0]
//
//    fun createStrategyRunner(money: Double, initialPrice: Double) = GridTableStrategyRunner(
//        initialPrice,
//        money,
//        runConfiguration.multiplier,
//        runConfiguration.symbol.symbol,
//        runConfiguration.diapason,
//        runConfiguration.gridSize,
//        runConfiguration.symbol.minPrice,
//        runConfiguration.symbol.minOrderQty
//    )
//
//
//    var strategyRunner = createStrategyRunner(runConfiguration.startCapital, initialPrice)
//    var checker =
//        TpAndSlChecker(runConfiguration.startCapital, runConfiguration.takeProfit, runConfiguration.stopLoss)
//    var skip = false
//    var newMoney = runConfiguration.startCapital
//
//    for (price in prices) {
//        if (!skip) {
//            skip = true
//            continue
//        }
//
//        strategyRunner.acceptPriceChange(price)
//
//        val pnl = strategyRunner.getPnL()
//        val checkResult = checker.checkPnLExceedingBoundsWithSlUpdating(pnl)
//
//        if (checkResult == TP || checkResult == SL) {
//            newMoney += pnl
//            strategyRunner = createStrategyRunner(newMoney, price)
//            checker = TpAndSlChecker(newMoney, runConfiguration.takeProfit, runConfiguration.stopLoss)
//        }
//    }
//
//    return newMoney + strategyRunner.getPnL()
//}
