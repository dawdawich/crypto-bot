package space.dawdawich.exception

class UnsupportedSymbolException(symbol: String) : RuntimeException("Unsupported symbol: $symbol")
