package space.dawdawich.model

import space.dawdawich.model.constants.AnalyzerChooseStrategy
import space.dawdawich.model.constants.Market


data class RequestProfitableAnalyzer(
        val accountId: String,
        val chooseStrategy: AnalyzerChooseStrategy,
        val currentAnalyzerId: String? = null,
        val managerMoney: Double,
        val demoAccount: Boolean,
        val market: Market
)
