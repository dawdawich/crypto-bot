package space.dawdawich.common

class TpAndSlChecker(
    private val startCapital: Double,
    private val takeProfit: Int,
    private val stopLoss: Int,
) {
    private var actualStopLoss = stopLoss
    private var maxProfitPercent = 0.0

    /**
     * @param pNl absolute PnL in same units as startCapital (e.g. USDT)
     * @return TP if target profit hit, SL if stop-loss hit, NONE otherwise
     */
    fun checkPnLExceedingBoundsWithSlUpdating(pNl: Double): CheckResult {
        // 1) Convert to percent PnL
        val percentPnL = pNl / startCapital * 100

        // 2) If we've reached a new profit high, tighten SL by 1% per full % profit
        if (percentPnL > maxProfitPercent) {
            maxProfitPercent = percentPnL
            val tightenBy = maxProfitPercent.toInt()              // floors toward zero
            actualStopLoss = (stopLoss - tightenBy).coerceAtLeast(0)
        }

        // 3) Check Take-Profit first
        if (percentPnL >= takeProfit) {
            return CheckResult.TP
        }

        // 4) Check Stop-Loss next (note: uses tightened actualStopLoss)
        if (percentPnL <= -actualStopLoss) {
            return CheckResult.SL
        }

        // 5) Otherwise, nothing to do
        return CheckResult.NONE
    }

    enum class CheckResult {
        TP, SL, NONE
    }
}
