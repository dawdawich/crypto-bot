package space.dawdawich.model

import space.dawdawich.model.constants.AnalyzerChooseStrategy


data class RequestProfitableAnalyzer(
        val accountId: String?,
        val chooseStrategy: AnalyzerChooseStrategy,
        val currentAnalyzerId: String? = null,
        val managerMoney: Double
)
