package space.dawdawich.repositories.custom.model

data class AnalyzerFilter(
    val statusFilter: Boolean?,
    val symbolFilter: List<String>
)
