package space.dawdawich.data

enum class Trend(val direction: Int, val directionName: String) {
    LONG(1, "Buy"), SHORT(-1, "Sell")

}
