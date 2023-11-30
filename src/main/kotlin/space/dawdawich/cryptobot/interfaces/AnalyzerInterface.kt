package space.dawdawich.cryptobot.interfaces

import space.dawdawich.cryptobot.data.AnalyzerData
import space.dawdawich.cryptobot.data.OpenOrderInfo
import space.dawdawich.cryptobot.manager.OrderManager
import java.util.function.Consumer

interface AnalyzerInterface : Consumer<Double> {
    fun getAnalyzerInfo(): AnalyzerData
    fun getInfoForOrder(): OpenOrderInfo
    fun setupManager(manager: OrderManager?)
    fun terminateOrder()
    fun hasManager(): Boolean
}
