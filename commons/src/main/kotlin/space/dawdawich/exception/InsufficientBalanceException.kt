package space.dawdawich.exception

/**
 * The `InsufficientBalanceException` class is an exception that is thrown when there is not enough balance to perform an analyzer activation.
 *
 * @property message A description of the exception.
 * @constructor Creates an `InsufficientBalanceException` with the given message.
 */
class InsufficientBalanceException: Exception("Not enough balance to make this operation")
