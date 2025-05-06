package space.dawdawich.model

data class BackTestResult(
    val runConfiguration: BackTestConfiguration,
    val startTime: Long,
    val endTime: Long,
    val result: Double
)
