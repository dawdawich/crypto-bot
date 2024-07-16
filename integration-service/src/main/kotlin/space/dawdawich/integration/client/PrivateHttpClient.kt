package space.dawdawich.integration.client

import io.ktor.client.plugins.*
import space.dawdawich.integration.model.PositionInfo

interface PrivateHttpClient : PublicHttpClient {

    @Throws(HttpRequestTimeoutException::class)
    suspend fun createOrder(
        symbol: String,
        entryPrice: Double,
        qty: Double,
        isLong: Boolean,
        orderId: String,
        positionIdx: Int = 0,
        repeatCount: Int = 0,
        isLimitOrder: Boolean
    ): Boolean

    suspend fun cancelAllOrder(symbol: String, repeatCount: Int = 2)
    suspend fun cancelOrder(symbol: String, orderId: String, repeatCount: Int = 2): Boolean
    suspend fun getAccountBalance(repeatCount: Int = 2): Double
    suspend fun getPositionInfo(symbol: String, retryCount: Int = 2): List<PositionInfo>
    suspend fun setMarginMultiplier(symbol: String, multiplier: Int, retryCount: Int = 2)
    suspend fun closePosition(symbol: String, isLong: Boolean, size: Double, positionIdx: Int = 0, repeatCount: Int = 2)
}
