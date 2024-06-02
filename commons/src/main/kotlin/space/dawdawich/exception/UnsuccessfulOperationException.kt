package space.dawdawich.exception

/**
 * This exception is thrown when an API operation is unsuccessful.
 *
 * @property statusCode The status code of the unsuccessful operation.
 */
class UnsuccessfulOperationException(statusCode: Int):
    Exception("Operation was unsuccessful. Operation status code '$statusCode'")
