package space.dawdawich.exception.model

/**
 * This exception is thrown when a record already exists.
 *
 * @param message The detailed error message.
 * @param cause The cause of the exception.
 */
class RecordAlreadyExistsException(message: String, cause: Throwable? = null) : Exception(message, cause)
