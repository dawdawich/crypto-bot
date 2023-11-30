package space.dawdawich.service

import com.jayway.jsonpath.ParseContext
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.json.JSONObject
import org.springframework.stereotype.Service

@Service
class ByBitOrderHttpService(private val httpClient: HttpClient, private val jsonPath: ParseContext) {

    companion object {
        const val BYBIT_URL = "https://api.bybit.com/v5"
        const val CREATE_ORDER_URL = "$BYBIT_URL/order/create"
        const val GET_ACCOUNT_BALANCE = "$BYBIT_URL/account/wallet-balance"
        const val GET_INSTRUMENTS_INFO = "$BYBIT_URL/market/instruments-info"
    }

    suspend fun createOrder(symbol: String, entryPrice: Double, qty: Double, isLong: Boolean, orderId: String, apiKey: String, timestamp: Long, getSign: (String) -> String): String? {
        val body = JSONObject(
            mapOf(
                "symbol" to symbol,
                "side" to if (isLong) "Buy" else "Sell",
                "orderType" to "Market",
                "qty" to qty.toString(),
                "triggerPrice" to entryPrice.toString(),
                "triggerDirection" to if (isLong) 2 else 1,
                "timeInForce" to "IOC",
                "triggerBy" to "MarkPrice",
                "category" to "linear",
                "isLeverage" to 1,
                "positionIdx" to 0,
                "orderLinkId" to orderId
            )
        ).toString()

        return httpClient.post(CREATE_ORDER_URL) {
            setBody(body)
            headers {
                append("X-BAPI-API-KEY", apiKey)
                append("X-BAPI-SIGN", getSign(body))
                append("X-BAPI-SIGN-TYPE", "2")
                append("X-BAPI-TIMESTAMP", timestamp.toString())
                append("X-BAPI-RECV-WINDOW", "5000")
                append("Content-Type", "application/json")
            }
        }.bodyAsText()
    }

    suspend fun getAccountBalance(apiKey: String, timestamp: Long, getSign: (String) -> String): String {
        return httpClient.get("$GET_ACCOUNT_BALANCE?accountType=CONTRACT&coin=USDT") {
            headers {
                append("X-BAPI-API-KEY", apiKey)
                append("X-BAPI-SIGN", getSign("accountType=CONTRACT&coin=USDT"))
                append("X-BAPI-SIGN-TYPE", "2")
                append("X-BAPI-TIMESTAMP", timestamp.toString())
                append("X-BAPI-RECV-WINDOW", "5000")
                append("Content-Type", "application/json")
            }
        }.bodyAsText()
    }

    suspend fun getPairInstructions(symbol: String): Pair<Double, Double> {
        val response = httpClient.get("$GET_INSTRUMENTS_INFO?category=linear&symbol=$symbol").bodyAsText()

        val parsedRes = jsonPath.parse(response)
        return parsedRes.read<String>("\$.result.list[0].lotSizeFilter.qtyStep").toDouble() to parsedRes.read<String>("\$.result.list[0].priceFilter.minPrice").toDouble()
    }
}
