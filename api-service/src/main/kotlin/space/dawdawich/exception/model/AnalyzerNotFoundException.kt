package space.dawdawich.exception.model

/**
 * Exception thrown when an analyzer is not found.
 *
 * @param message The detail message.
 * @param cause The cause of the exception.
 */
class AnalyzerNotFoundException(message: String, cause: Throwable? = null) : Exception(message, cause)
