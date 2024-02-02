package space.dawdawich.model.analyzer

enum class Trend(val direction: Int, val directionName: String, val directionBoolean: Boolean) {
    LONG(1, "Buy", true), SHORT(-1, "Sell", false);

    companion object {
        fun fromDirection(direction: String) = when (direction.lowercase()) {
            LONG.directionName.lowercase() -> LONG
            SHORT.directionName.lowercase() -> SHORT
            else -> throw IllegalArgumentException("Passes illegal argument direction. Provided value: '$direction'")
        }
    }
}
