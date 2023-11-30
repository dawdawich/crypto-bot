package space.dawdawich.cryptobot.analyzer

import space.dawdawich.cryptobot.data.AnalyzerData
import space.dawdawich.cryptobot.data.OpenOrderInfo
import space.dawdawich.cryptobot.data.Order
import space.dawdawich.cryptobot.data.Trend
import space.dawdawich.cryptobot.interfaces.AnalyzerInterface
import space.dawdawich.cryptobot.manager.OrderManager
import java.util.*

abstract class AnalyzerCore(
    var wallet: Double = 100.0,
    val stopLossPercent: Float,
    val takeProfitPercent: Float,
    val ticksToSwitch: Int = 1,
    var trend: Trend = Trend.BULL,
    var currentPrice: Double = 0.0,
    val multiplier: Int = 1,
    val pair: String = "BTCUSDT",
    val id: String = UUID.randomUUID().toString(),
    var manager: OrderManager? = null
) : AnalyzerInterface {
    protected var switchCounter = 0
    protected var previousPrice = currentPrice
    protected var stopLossRaiser = 0
    protected var order: Order? = null
    var profitOrdersCount = 0
    var loseOrdersCount = 0

    override fun getAnalyzerInfo(): AnalyzerData {
        return AnalyzerData(wallet, stopLossPercent, takeProfitPercent, ticksToSwitch, multiplier, pair)
    }

    override fun getInfoForOrder(): OpenOrderInfo {
        return OpenOrderInfo(trend, stopLossPercent, takeProfitPercent, multiplier, currentPrice)
    }

    override fun hasManager(): Boolean = manager != null

    override fun setupManager(manager: OrderManager?) {
        this.manager = manager
    }

    fun getOrderCompletionFactor(): Int = profitOrdersCount - loseOrdersCount
}
