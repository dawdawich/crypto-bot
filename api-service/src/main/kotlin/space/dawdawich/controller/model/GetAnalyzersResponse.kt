package space.dawdawich.controller.model

data class GetAnalyzersResponse(val analyzers: List<GridTableAnalyzerResponse>, val totalSize: Int = 0)
