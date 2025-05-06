package space.dawdawich.strategy

abstract class StrategyRunner(
    var money: Double,
    protected val multiplier: Int,
    protected val minQtyOrder: Double,
    val symbol: String,
    val id: String,
) {
    abstract fun getUnrializedPnL(): Double
    abstract fun getPnL(): Double


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StrategyRunner) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
