package space.dawdawich.controller.model.analyzer

/**
 * Represents the response object for obtaining analyzers.
 *
 * @property analyzers The list of grid table analyzers.
 * @property totalSize The total size of analyzers.
 * @property activeSize The size of active analyzers.
 * @property notActiveSize The size of not active analyzers.
 */
data class GetAnalyzersResponse(val analyzers: List<GridTableAnalyzerResponse>, val totalSize: Int = 0, val activeSize: Int = 0, val notActiveSize: Int = 0)
