package space.dawdawich.service

import com.jayway.jsonpath.ParseContext
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.headers
import io.ktor.client.statement.*
import org.json.JSONObject
import space.dawdawich.service.model.Position
import space.dawdawich.utils.bytesToHex
import javax.crypto.Mac

class ByBitHttpService(private val httpClient: HttpClient, private val jsonPath: ParseContext, private val apiKey: String, private val encryptor: Mac) {

    companion object {
        private const val BYBIT_URL = "https://api.bybit.com/v5"
        const val CREATE_ORDER_URL = "$BYBIT_URL/order/create"
        const val GET_ACCOUNT_BALANCE = "$BYBIT_URL/account/wallet-balance"
        const val GET_INSTRUMENTS_INFO = "$BYBIT_URL/market/instruments-info"
        const val SET_MARGIN = "$BYBIT_URL/position/set-leverage"
        const val CANCEL_ALL_ORDERS = "$BYBIT_URL/order/cancel-all"
        const val CANCEL_ORDER = "$BYBIT_URL/order/cancel"
        const val GET_POSITIONS = "$BYBIT_URL/position/list"
        const val SWITCH_MODE_POSITION = "$BYBIT_URL/position/switch-mode"
    }

    suspend fun createOrder(symbol:
                            String, entryPrice: Double, qty: Double, isLong: Boolean, positionIdx: Int, orderId: String, triggerDirection: Int): String? {
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
                "positionIdx" to positionIdx,
                "orderLinkId" to orderId,
                "triggerDirection" to triggerDirection
            )
        ).toString()

        val response = httpClient.post(CREATE_ORDER_URL, setRequestParams(body)).bodyAsText()
        val res = jsonPath.parse(response)
        if (res.read<Int>("\$.retCode") == 110009) {
            cancelAllOrder(symbol)
            return createOrder(symbol, entryPrice, qty, isLong, positionIdx, orderId, triggerDirection)
        }
        return res.read<String?>("\$.result.orderLinkId") ?: run {
            println("Failed create response: $response")
            null
        }
    }

    suspend fun getAccountBalance(): Double {
        return jsonPath.parse(httpClient.get("$GET_ACCOUNT_BALANCE?accountType=CONTRACT&coin=USDT") {
            getByBitHeadersWithSign("accountType=CONTRACT&coin=USDT")
        }.bodyAsText()).run {
            read<String?>("\$.result.list[0].coin[0].walletBalance")?.toDouble() ?: 0.0
        }
    }

    suspend fun getPairInstructions(symbol: String): Pair<Double, Double> {
        val response = httpClient.get("$GET_INSTRUMENTS_INFO?category=linear&symbol=$symbol").bodyAsText()

        val parsedRes = jsonPath.parse(response)
        return parsedRes.read<String>("\$.result.list[0].lotSizeFilter.qtyStep").toDouble() to parsedRes.read<String>("\$.result.list[0].priceFilter.minPrice").toDouble()
    }

    suspend fun getPositionsInfo(symbol: String): List<Position> {
        val bodyAsText = httpClient.get("$GET_POSITIONS?category=linear&symbol=$symbol") {
            getByBitHeadersWithSign("category=linear&symbol=$symbol")
        }.bodyAsText()

        println(bodyAsText)
        return jsonPath.parse(bodyAsText).let {
            if (it.read<Int?>("\$.retCode") == 0) {
                return@let it.read<List<Map<String, Any>>>("\$.result.list").map { position ->
                    Position(
                        position["symbol"].toString(),
                        position["side"].toString().equals("buy", true),
                        position["size"].toString().toDouble(),
                        0.0,
                        position["positionIdx"].toString().toInt(),
                        position["updatedTime"].toString().toLong()
                    )
                }
            } else listOf()
        }
    }

    suspend fun setMarginMultiplier(symbol: String, multiplayer: Int): Boolean {
        val body = JSONObject(
            mapOf(
                "category" to "linear",
                "symbol" to symbol,
                "buyLeverage" to multiplayer.toString(),
                "sellLeverage" to multiplayer.toString()
            )
        ).toString()

        return jsonPath.parse(httpClient.post(SET_MARGIN, setRequestParams(body)).bodyAsText()).run {
            read<Int>("\$.retCode").let { return@run  it == 0 || it == 110043 }
        }
    }

    suspend fun cancelAllOrder(symbol: String) {
        val body = JSONObject(
            mapOf(
                "category" to "linear",
                "symbol" to symbol
            )
        ).toString()

        httpClient.post(CANCEL_ALL_ORDERS, setRequestParams(body))
    }

    suspend fun cancelOrder(symbol: String, linkOrderId: String) {
        val body = JSONObject(
            mapOf(
                "category" to "linear",
                "symbol" to symbol,
                "orderLinkId" to linkOrderId
            )
        ).toString()

        httpClient.post(CANCEL_ORDER, setRequestParams(body))
    }

    suspend fun closePosition(symbol: String, isLong: Boolean, size: Double, positionIdx: Int) {
        val body = JSONObject(
            mapOf(
                "symbol" to symbol,
                "side" to if (isLong) "Sell" else "Buy",
                "orderType" to "Market",
                "qty" to size.toString(),
                "positionIdx" to positionIdx.toString(),
                "reduceOnly" to true,
                "closeOnTrigger" to true,
                "category" to "linear",
                "isLeverage" to 1
            )
        ).toString()

        val bodyAsText = httpClient.post(CREATE_ORDER_URL, setRequestParams(body)).bodyAsText()

        println("Close position response: $bodyAsText")

        jsonPath.parse(bodyAsText)?.let { it.read<Int?>("\$.retCode") == 0 }
    }

    suspend fun switchPosition(symbol: String) {
        val body = JSONObject(
            mapOf(
                "symbol" to symbol,
                "category" to "linear",
                "mode" to 3
            )
        ).toString()

        val bodyAsText = httpClient.post(SWITCH_MODE_POSITION, setRequestParams(body)).bodyAsText()

        println("Switch position response: $bodyAsText")

        jsonPath.parse(bodyAsText)?.let { it.read<Int?>("\$.retCode") == 0 }
    }

    private fun HttpRequestBuilder.getByBitHeadersWithSign(body: String) {
        val timestamp = System.currentTimeMillis().toString()
        headers {
            append("X-BAPI-API-KEY", apiKey)
            append("X-BAPI-SIGN", getSign(body, timestamp))
            append("X-BAPI-SIGN-TYPE", "2")
            append("X-BAPI-TIMESTAMP", timestamp)
            append("X-BAPI-RECV-WINDOW", "5000")
            append("Content-Type", "application/json")
        }
    }

    private fun setRequestParams(body: String): HttpRequestBuilder.() -> Unit = {
        setBody(body)
        getByBitHeadersWithSign(body)
    }

    private fun getSign(body: String, timestamp: String): String {
        return encryptor.doFinal("$timestamp${apiKey}5000$body".toByteArray()).bytesToHex()
    }
}
