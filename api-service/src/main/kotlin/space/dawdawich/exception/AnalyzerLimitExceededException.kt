package space.dawdawich.exception

/**
 * Exception thrown when the maximum number of active analyzers is exceeded.
 */
class AnalyzerLimitExceededException : Exception("Maximum number of active analyzers exceeded")
