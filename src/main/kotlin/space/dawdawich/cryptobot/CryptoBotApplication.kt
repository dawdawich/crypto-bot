package space.dawdawich.cryptobot

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import space.dawdawich.cryptobot.analyzer.AnalyzerCore
import space.dawdawich.cryptobot.analyzer.AutomaticSwitchTradingAnalyzer
import space.dawdawich.cryptobot.analyzer.GridTableAnalyzer
import space.dawdawich.cryptobot.analyzer.VoltyExpanCloseStrategyAnalyzer
import space.dawdawich.cryptobot.client.ByBitOrderWebSocketClient
import space.dawdawich.cryptobot.client.BybitTickerWebSocketClient
import space.dawdawich.cryptobot.data.KLineIntervals
import space.dawdawich.cryptobot.interfaces.AnalyzerInterface
import space.dawdawich.cryptobot.manager.GridOrderManager
import space.dawdawich.cryptobot.manager.OrderManager
import space.dawdawich.cryptobot.service.*
import space.dawdawich.cryptobot.util.HttpUtils
import space.dawdawich.cryptobot.util.plusPercent
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZonedDateTime
import java.util.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val logger = KotlinLogging.logger {}

val pairs = listOf("BTCUSDT", "ETHUSDT", "SOLUSDT", "GASUSDT", "SUSHIUSDT", "ARKUSDT", "TRBUSDT", "ARBUSDT", "CAKEUSDT")

val pairInstructions = mutableMapOf(*pairs.map { it to 0.0 }.toTypedArray())
val pairMinPriceInstructions = mutableMapOf(*pairs.map { it to 0.0.toBigDecimal() }.toTypedArray())
var startCapital = 10.0

val balanceRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
val pnlRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
val accBalanceRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

suspend fun main() {
    println(ZonedDateTime.now())

    Metrics.addRegistry(balanceRegistry)
    Metrics.addRegistry(pnlRegistry)
    Metrics.addRegistry(accBalanceRegistry)

    startCapital = OrderManagerService.getAccountBalance()

    logger.info { "Start capital is '$startCapital'" }

    pairInstructions.keys.forEach {
        val pairInfo = OrderManagerService.getMinQty(it)
        pairInstructions[it] = pairInfo.first
        pairMinPriceInstructions[it] = pairInfo.second.toBigDecimal()
        runBlocking { delay(0.5.seconds) }
    }

    val tickerClient = BybitTickerWebSocketClient(pairs)
    val orderClient = ByBitOrderWebSocketClient()


    try {
        val savedAnalyzers: MutableList<GridTableAnalyzer> = mutableListOf()
        pairs.forEach { pair ->
            val analyzers = mutableSetOf<GridTableAnalyzer>()
            generateGridBotAnalyzers(analyzers, pair)
            analyzers.forEach { tickerClient.addSubscriber(pair, it) }
            savedAnalyzers.addAll(analyzers)
        }

        initBalanceMetrics(tickerClient.getSubscribers())

        tickerClient.connectBlocking()
        logger.info { "Connected to tickers websocket" }



        logger.info { "Started API server" }
        startEmbeddedServer { savedAnalyzers }
//        runCatching {
//            delay(30.minutes)
//
//            logger.info { "Started crypto bot" }
//            val gridOrderManager = GridOrderManager(savedAnalyzers, startCapital)
//            orderClient.subscribers.add(gridOrderManager)
//            orderClient.connectBlocking()
//            Gauge.builder("acc.balance.gauge") { gridOrderManager.money }.register(accBalanceRegistry)
//            gridOrderManager.start()
//        }
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }
}

private fun generateTickerAnalyzers(analyzers: MutableSet<AutomaticSwitchTradingAnalyzer>, pair: String) {
    for (i in 2..5) {
        for (j in 2..10) {
            for (t in 1..7) {
                for (m in 1..20) {
                    analyzers.add(
                        AutomaticSwitchTradingAnalyzer(
                            wallet = startCapital,
                            stopLossPercent = i.toFloat(),
                            takeProfitPercent = j.toFloat(),
                            ticksToSwitch = t,
                            multiplier = m,
                            pair = pair
                        )
                    )
                }
            }
        }
    }
}

private fun generateGridBotAnalyzers(analyzers: MutableSet<GridTableAnalyzer>, pair: String) {
    for (stopLoss in 5..8) {
        for (takeProfit in 7..11) {
            for (diapasonPercent in 1..3) {
                for (gridSize in 10..150 step 10) {
                    for (multiplier in 15..25) {
                        analyzers += GridTableAnalyzer(
                            diapason = diapasonPercent,
                            gridSize = gridSize,
                            startCapital,
                            multiplier,
                            stopLoss,
                            takeProfit,
                            pair
                        )
                    }
                }
            }
        }
    }
}

private fun generateAndFillVoltyAnalyzers(analyzers: MutableSet<VoltyExpanCloseStrategyAnalyzer>, pair: String) {
    for (stopLoss in 3..5) {
        for (takeProfit in 3..5) {
            for (multiplier in 1..30) {
                for (interval in listOf(
                    KLineIntervals.`1`,
                    KLineIntervals.`3`,
                    KLineIntervals.`5`,
                    KLineIntervals.`15`
                )) {
                    for (statisticLength in 5..60 step 5) {
                        for (y in 1..6) {
                            analyzers.add(
                                VoltyExpanCloseStrategyAnalyzer(
                                    wallet = startCapital,
                                    stopLossPercent = stopLoss.toFloat(),
                                    takeProfitPercent = takeProfit.toFloat(),
                                    multiplier = multiplier,
                                    pair = pair,
                                    candleInterval = interval,
                                    statisticLength = statisticLength,
                                    yFactor = y * 0.5
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

fun startEmbeddedServer(getAnalyzers: () -> List<AnalyzerInterface>) {
    embeddedServer(Netty, port = 8383) {
        routing {
            get("/metrics-balance") {
                reinitializeBalanceMetrics(getAnalyzers().filterIsInstance<GridTableAnalyzer>())
                call.respondText(balanceRegistry.scrape(), contentType = ContentType.Text.Plain)
            }
            get("/metrics-pnl") {
                reinitializePnLMetrics(getAnalyzers().filterIsInstance<GridTableAnalyzer>())
                call.respondText(pnlRegistry.scrape(), contentType = ContentType.Text.Plain)
            }
            get("/metrics-account-balance") {
                call.respondText(accBalanceRegistry.scrape(), contentType = ContentType.Text.Plain)
            }
        }
    }.start(wait = false)
}
