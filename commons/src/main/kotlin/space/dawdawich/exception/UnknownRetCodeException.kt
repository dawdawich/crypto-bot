package space.dawdawich.exception

/**
 * Exception thrown when an unknown return code is encountered from market. After detected this exception,
 * unknown ret code should be added to list and clearly processed.
 *
 * @param retCode The unknown return code.
 */
class UnknownRetCodeException(retCode: Int) : Exception("Unknown return code: $retCode")
