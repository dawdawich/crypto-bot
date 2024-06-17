package space.dawdawich.repositories.custom.mongo.model

data class AnalyzerFilter(
    val statusFilter: Boolean?,
    val symbolFilter: List<String>
)
