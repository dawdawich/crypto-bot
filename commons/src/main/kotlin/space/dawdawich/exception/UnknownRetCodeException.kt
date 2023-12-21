package space.dawdawich.exception

class UnknownRetCodeException(retCode: Int) : Exception("Unknown return code: $retCode")
