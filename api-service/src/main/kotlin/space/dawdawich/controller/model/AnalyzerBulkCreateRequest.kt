package space.dawdawich.controller.model

data class AnalyzerBulkCreateRequest(
    val symbols: List<String>,
    val minStopLoss: Int,
    val maxStopLoss: Int,
    val minTakeProfit: Int,
    val maxTakeProfit: Int,
    val startDiapasonPercent: Int,
    val endDiapasonPercent: Int,
    val fromGridSize: Int,
    val toGridSize: Int,
    val gridSizeStep: Int,
    val multiplierFrom: Int,
    val multiplierTo: Int,
    val startCapital: Int
)
