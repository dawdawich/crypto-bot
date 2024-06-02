package space.dawdawich.exception.model

/**
 * This exception is thrown when a folder is not found.
 *
 * @param message The detailed error message.
 * @param cause The cause of the exception.
 */
class FolderNotFoundException(message: String, cause: Throwable? = null) : Exception(message, cause)
