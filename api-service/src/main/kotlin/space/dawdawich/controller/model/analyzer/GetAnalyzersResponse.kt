package space.dawdawich.controller.model.analyzer

data class GetAnalyzersResponse(val analyzers: List<GridTableAnalyzerResponse>, val totalSize: Int = 0, val activeSize: Int = 0, val notActiveSize: Int = 0)
