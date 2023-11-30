package space.dawdawich.cryptobot.service

import io.micrometer.core.instrument.Gauge
import space.dawdawich.cryptobot.analyzer.GridTableAnalyzer
import space.dawdawich.cryptobot.balanceRegistry
import space.dawdawich.cryptobot.pnlRegistry
import space.dawdawich.cryptobot.startCapital

fun initBalanceMetrics(analyzers: List<GridTableAnalyzer>) {
    analyzers.filter { it.calculateMoneyWithPositions() > startCapital }.sortedByDescending { it.calculateMoneyWithPositions() }.take(20).forEach {
        Gauge.builder("balance.grid.gauge") { it.calculateMoneyWithPositions() }
            .tag("id", it.id)
            .tag("stop_loss", it.stopLoss.toString())
            .tag("take_profit", it.takeProfit.toString())
            .tag("multiplier", it.multiplier.toString())
            .tag("pair", it.pair)
            .tag("diapason", it.diapason.toString())
            .tag("grid_size", it.gridSize.toString())
            .register(balanceRegistry)
    }
}

fun initPnLMetrics(analyzers: List<GridTableAnalyzer>) {
    analyzers.filter { it.calculateMoneyWithPositions() > startCapital }.sortedByDescending { it.pnlFactor }.take(20).forEach {
        Gauge.builder("pnl.grid.gauge") { it.pnlFactor }
            .tag("id", it.id)
            .tag("stop_loss", it.stopLoss.toString())
            .tag("take_profit", it.takeProfit.toString())
            .tag("multiplier", it.multiplier.toString())
            .tag("pair", it.pair)
            .tag("diapason", it.diapason.toString())
            .tag("grid_size", it.gridSize.toString())
            .register(pnlRegistry)
    }
}

fun reinitializeBalanceMetrics(analyzers: List<GridTableAnalyzer>) {
    balanceRegistry.clear()
    initBalanceMetrics(analyzers)
}

fun reinitializePnLMetrics(analyzers: List<GridTableAnalyzer>) {
    pnlRegistry.clear()
    initPnLMetrics(analyzers)
}
