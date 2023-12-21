package space.dawdawich.exception

class UnsuccessfulOperationException(statusCode: Int): Exception("Operation was unsuccessful. Operation status code '$statusCode'")
