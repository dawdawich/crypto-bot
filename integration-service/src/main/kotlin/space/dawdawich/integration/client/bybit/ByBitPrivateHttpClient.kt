package space.dawdawich.integration.client.bybit

import com.jayway.jsonpath.ParseContext
import space.dawdawich.integration.model.PositionInfo
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mu.KotlinLogging
import space.dawdawich.exception.ReduceOnlyRuleNotSatisfiedException
import space.dawdawich.exception.UnknownRetCodeException
import space.dawdawich.utils.bytesToHex
import javax.crypto.Mac

class ByBitPrivateHttpClient(
    serverUrl: String,
    client: HttpClient,
    jsonPath: ParseContext,
    private val apiKey: String,
    private val encryptor: Mac
) : ByBitPublicHttpClient(serverUrl, client, jsonPath) {
    companion object {
        const val CREATE_ORDER_URL = "/order/create"
        const val GET_ACCOUNT_BALANCE = "/account/wallet-balance"
        const val SET_MARGIN = "/position/set-leverage"
        const val CANCEL_ALL_ORDERS = "/order/cancel-all"
        const val CANCEL_ORDER = "/order/cancel"
        const val GET_POSITIONS = "/position/list"
    }

    private val logger = KotlinLogging.logger {}

    suspend fun createOrder(
        symbol:
        String,
        entryPrice: Double,
        qty: Double,
        isLong: Boolean,
        positionIdx: Int,
        orderId: String,
        triggerDirection: Int,
        repeatCount: Int = 2
    ): Boolean {
        val request = buildJsonObject {
            put("symbol", symbol)
            put("side", if (isLong) "Buy" else "Sell")
            put("orderType", "Market")
            put("qty", qty.toString())
            put("triggerPrice", entryPrice.toString())
            put("triggerDirection", if (isLong) 2 else 1)
            put("timeInForce", "IOC")
            put("triggerBy", "MarkPrice")
            put("category", "linear")
            put("isLeverage", 1)
            put("positionIdx", positionIdx)
            put("orderLinkId", orderId)
            put("triggerDirection", triggerDirection)
        }.toString()

        try {
            val response = repeatCount repeatTry { post(CREATE_ORDER_URL, request, getByBitHeadersWithSign(request)) }

            val parsedJson = jsonPath.parse(response.bodyAsText())
            when (val returnCode = parsedJson.read<Int>("\$.retCode")) {
                0 -> return true
                10001, 10002 -> return false
                110009 -> {
                    cancelAllOrder(symbol)
                    return createOrder(symbol, entryPrice, qty, isLong, positionIdx, orderId, triggerDirection)
                }

                else -> {
                    throw UnknownRetCodeException(returnCode)
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to create order on bybit server" }
        }
        return false
    }

    suspend fun cancelAllOrder(symbol: String, repeatCount: Int = 2) {
        val request = buildJsonObject {
            put("category", "linear")
            put("symbol", symbol)
        }.toString()

        try {
            val response = repeatCount repeatTry { post(CANCEL_ALL_ORDERS, request, getByBitHeadersWithSign(request)) }

            val parsedJson = jsonPath.parse(response.bodyAsText())
            val returnCode = parsedJson.read<Int>("$.retCode")
            if (returnCode != 0) {
                throw UnknownRetCodeException(returnCode)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getAccountBalance(repeatCount: Int = 2): Double {
        val query = "accountType=UNIFIED&coin=USDT"
        val response: HttpResponse = repeatCount repeatTry { get(GET_ACCOUNT_BALANCE, query, getByBitHeadersWithSign(query)) }
        val parsedJson = jsonPath.parse(response.bodyAsText())

        when (val returnCode = parsedJson.read<Int>("\$.retCode")) {
            0 -> {
                return parsedJson.read<String>("$.result.list[0].totalEquity").toDouble()
            }

            else -> {
                throw UnknownRetCodeException(returnCode)
            }
        }
    }


    suspend fun getPositionInfo(symbol: String): List<PositionInfo> {
        val query = "category=linear&symbol=$symbol"
        val headers = getByBitHeadersWithSign(query)
        val response = get(GET_POSITIONS, query, headers)
        val parsedJson = jsonPath.parse(response.bodyAsText())

        when (val returnCode = parsedJson.read<Int>("\$.retCode")) {
            0 -> {
                return parsedJson.read<List<Map<String, Any>>>("\$.result.list").map { position ->
                    PositionInfo(
                        position["symbol"].toString(),
                        position["side"].toString().equals("buy", true),
                        position["size"].toString().toDouble(),
                        position["positionValue"].toString().let { if (it.isBlank()) 0.0 else it.toDouble() },
                        position["positionIdx"].toString().toInt(),
                        position["updatedTime"].toString().let { if (it.isBlank()) 0 else it.toLong() }
                    )
                }
            }

            else -> {
                throw UnknownRetCodeException(returnCode)
            }
        }
    }

    suspend fun setMarginMultiplier(symbol: String, multiplayer: Int, retryCount: Int = 2): Boolean {
        val request: String = buildJsonObject {
            put("category", "linear")
            put("symbol", symbol)
            put("buyLeverage", multiplayer.toString())
            put("sellLeverage", multiplayer.toString())
        }.toString()
        try {
            val response: HttpResponse =  retryCount repeatTry { post(SET_MARGIN, request, getByBitHeadersWithSign(request)) }


            val parsedJson = jsonPath.parse(response.bodyAsText())
            when (val returnCode = parsedJson.read<Int>("\$.retCode")) {
                0, 110043 -> {
                    return true
                }

                else -> {
                    throw UnknownRetCodeException(returnCode)
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun closePosition(symbol: String, isLong: Boolean, size: Double, positionIdx: Int, repeatCount: Int = 2) {
        val request: String = buildJsonObject {
            put("category", "linear")
            put("symbol", symbol)
            put("side", if (isLong) "Sell" else "Buy")
            put("qty", size.toString())
            put("positionIdx", positionIdx.toString())
            put("isLeverage", 1)
            put("reduceOnly", true)
            put("orderType", "Market")
            put("timeInForce", "IOC")
        }.toString()

        try {
            val response: HttpResponse = repeatCount repeatTry { post(CREATE_ORDER_URL, request, getByBitHeadersWithSign(request)) }

            val parsedJson = jsonPath.parse(response.bodyAsText())
            when (val returnCode = parsedJson.read<Int>("$.retCode")) {
                0 -> return
                110017 -> throw ReduceOnlyRuleNotSatisfiedException()
                else -> {
                    throw UnknownRetCodeException(returnCode)
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }


    private suspend infix fun <T> Int.repeatTry(block: suspend () -> T) : T {
        return try {
            block()
        } catch (timeoutEx: HttpRequestTimeoutException) {
            if (this > 0) {
                (this - 1).repeatTry(block)
            } else {
                throw timeoutEx
            }
        }

    }

    private fun getByBitHeadersWithSign(body: String): Array<Pair<String, String>> {
        val timestamp = System.currentTimeMillis().toString()
        return arrayOf(
            "X-BAPI-API-KEY" to apiKey,
            "X-BAPI-SIGN" to encryptor.doFinal("$timestamp${apiKey}5000$body".toByteArray()).bytesToHex(),
            "X-BAPI-SIGN-TYPE" to "2",
            "X-BAPI-TIMESTAMP" to timestamp,
            "X-BAPI-RECV-WINDOW" to "5000",
            "Content-Type" to "application/json"
        )
    }
}
