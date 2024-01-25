package space.dawdawich.strategy.model

enum class Trend(val direction: Int, val directionName: String) {
    LONG(1, "Buy"), SHORT(-1, "Sell");

    companion object {
        fun fromDirection(direction: String) = when (direction.lowercase()) {
            LONG.directionName.lowercase() -> LONG
            SHORT.directionName.lowercase() -> SHORT
            else -> throw IllegalArgumentException("Passes illegal argument direction. Provided value: '$direction'")
        }
    }
}
