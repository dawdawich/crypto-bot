package space.dawdawich.exception

/**
 * This class represents an exception that is thrown when attempting to set up a margin
 * that is not supported by the symbol. Depends on markets configs.
 *
 * @param message The error message associated with the exception.
 */
class InvalidMarginProvided: Exception("Try to set up margin, which not supported by symbol")
