package space.dawdawich.integration.client.bybit

import com.jayway.jsonpath.ParseContext
import space.dawdawich.integration.model.PositionInfo
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mu.KotlinLogging
import space.dawdawich.exception.*
import space.dawdawich.integration.client.PrivateHttpClient
import space.dawdawich.utils.bytesToHex
import java.nio.channels.UnresolvedAddressException
import javax.crypto.Mac
import kotlin.jvm.Throws

private const val RET_CODE_KEY = "\$.retCode"

class ByBitPrivateHttpClient(
    serverUrl: String,
    client: HttpClient,
    jsonPath: ParseContext,
    private val apiKey: String,
    private val encryptor: Mac,
) : ByBitPublicHttpClient(serverUrl, client, jsonPath), PrivateHttpClient {
    companion object {
        const val CREATE_ORDER_URL = "/order/create"
        const val GET_ACCOUNT_BALANCE = "/account/wallet-balance"
        const val SET_MARGIN = "/position/set-leverage"
        const val CANCEL_ALL_ORDERS = "/order/cancel-all"
        const val CANCEL_ORDER = "/order/cancel"
        const val GET_POSITIONS = "/position/list"
    }

    private val logger = KotlinLogging.logger {}

    @Throws(HttpRequestTimeoutException::class)
    override suspend fun createOrder(
        symbol:
        String,
        entryPrice: Double,
        qty: Double,
        isLong: Boolean,
        orderId: String,
        positionIdx: Int,
        repeatCount: Int,
        isLimitOrder: Boolean,
    ): Boolean {
        val request = buildJsonObject {
            put("symbol", symbol)
            put("side", if (isLong) "Buy" else "Sell")
            put("orderType", if (isLimitOrder) "Limit" else "Market")
            put("qty", qty.toString())
            put("price", entryPrice.toString())
            put("timeInForce", "GTC")
            put("category", "linear")
            put("positionIdx", positionIdx)
            put("orderLinkId", orderId)
        }.toString()

        try {
            val response = repeatCount repeatTry { post(CREATE_ORDER_URL, request, getByBitHeadersWithSign(request)) }

            val parsedJson = jsonPath.parse(response.bodyAsText())
            when (val returnCode = parsedJson.read<Int>(RET_CODE_KEY)) {
                0 -> return true
                10002 -> createOrder(symbol, entryPrice, qty, isLong, orderId, positionIdx, repeatCount, true)
                10001 -> {
                    logger.info { "Failed to create order. Reason:\n${parsedJson.jsonString()}" }
                    return false
                }

                10004 -> throw InvalidSignatureException()
                110009 -> {
                    cancelAllOrder(symbol)
                    return createOrder(symbol, entryPrice, qty, isLong, orderId, isLimitOrder = true)
                }

                110007 -> throw InsufficientBalanceException()
                33004 -> throw ApiTokenExpiredException()
                else -> throw UnknownRetCodeException(returnCode)
            }
        } catch (e: UnresolvedAddressException) {
            logger.warn { "Failed to create order" }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to create order on bybit server" }
        }
        return false
    }

    override suspend fun cancelAllOrder(symbol: String, repeatCount: Int) {
        val request = buildJsonObject {
            put("category", "linear")
            put("symbol", symbol)
        }.toString()

        try {
            val response = repeatCount repeatTry { post(CANCEL_ALL_ORDERS, request, getByBitHeadersWithSign(request)) }

            val parsedJson = jsonPath.parse(response.bodyAsText())
            val returnCode = parsedJson.read<Int>(RET_CODE_KEY)
            if (returnCode == 10002) {
                cancelAllOrder(symbol, repeatCount)
            } else if (returnCode != 0) {
                throw UnknownRetCodeException(returnCode)
            }
        } catch (e: UnresolvedAddressException) {
            logger.warn { "Failed to cancel order" }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun cancelOrder(symbol: String, orderId: String, repeatCount: Int): Boolean {
        val request = buildJsonObject {
            put("category", "linear")
            put("symbol", symbol)
            put("orderLinkId", orderId)
        }.toString()

        try {
            val response = repeatCount repeatTry { post(CANCEL_ORDER, request, getByBitHeadersWithSign(request)) }

            val parsedJson = jsonPath.parse(response.bodyAsText())
            return when (val returnCode = parsedJson.read<Int>("$.retCode")) {
                0, 110001 -> true
                10002 -> return cancelOrder(symbol, orderId, repeatCount)
                10004 -> throw InvalidSignatureException()
                else -> throw UnknownRetCodeException(returnCode)
            }
        } catch (e: UnresolvedAddressException) {
            logger.warn { "Failed to cancel order" }
        } catch (e: Exception) {
            throw e
        }
        return false
    }

    override suspend fun getAccountBalance(repeatCount: Int): Double {
        val query = "accountType=UNIFIED&coin=USDT"
        val response: HttpResponse =
            repeatCount repeatTry { get(GET_ACCOUNT_BALANCE, query, getByBitHeadersWithSign(query)) }
        val bodyAsText = response.bodyAsText()
        logger.debug { "Account balance response: $bodyAsText" }
        val parsedJson = jsonPath.parse(bodyAsText)

        return when (val returnCode = parsedJson.read<Int>(RET_CODE_KEY)) {
            0 -> parsedJson.read<String>("$.result.list[0].coin[0].equity").toDouble()
            10002 -> getAccountBalance(repeatCount)
            10004 -> throw InvalidSignatureException()
            33004 -> throw ApiTokenExpiredException()
            else -> throw UnknownRetCodeException(returnCode)
        }
    }


    override suspend fun getPositionInfo(symbol: String, retryCount: Int): List<PositionInfo> {
        val query = "category=linear&symbol=$symbol"
        val headers = getByBitHeadersWithSign(query)
        val response = retryCount repeatTry { get(GET_POSITIONS, query, headers) }
        val parsedJson = jsonPath.parse(response.bodyAsText())

        return when (val returnCode = parsedJson.read<Int>(RET_CODE_KEY)) {
            0 -> parsedJson.read<List<Map<String, Any>>>("\$.result.list").map { position ->
                PositionInfo(
                    position["symbol"].toString(),
                    position["side"].toString().equals("buy", true),
                    position["size"].toString().toDouble(),
                    position["positionValue"].toString().let { if (it.isBlank()) 0.0 else it.toDouble() },
                    position["positionIdx"].toString().toInt(),
                    position["updatedTime"].toString().let { if (it.isBlank()) 0 else it.toLong() }
                )
            }

            10002 -> getPositionInfo(symbol, retryCount)
            10004 -> throw InvalidSignatureException()
            33004 -> throw ApiTokenExpiredException()
            else -> throw UnknownRetCodeException(returnCode)
        }
    }

    override suspend fun setMarginMultiplier(symbol: String, multiplier: Int, retryCount: Int) {
        val request: String = buildJsonObject {
            put("category", "linear")
            put("symbol", symbol)
            put("buyLeverage", multiplier.toString())
            put("sellLeverage", multiplier.toString())
        }.toString()
        val response: HttpResponse =
            retryCount repeatTry { post(SET_MARGIN, request, getByBitHeadersWithSign(request)) }

        val parsedJson = jsonPath.parse(response.bodyAsText())
        when (val returnCode = parsedJson.read<Int>(RET_CODE_KEY)) {
            0, 110043 -> return
            10001 -> throw InvalidMarginProvided()
            10002 -> setMarginMultiplier(symbol, multiplier, retryCount)
            10004 -> throw InvalidSignatureException()
            33004 -> throw ApiTokenExpiredException()
            else -> throw UnknownRetCodeException(returnCode)
        }
    }

    override suspend fun closePosition(
        symbol: String,
        isLong: Boolean,
        size: Double,
        positionIdx: Int,
        repeatCount: Int,
    ) {
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

        val response: HttpResponse =
            repeatCount repeatTry { post(CREATE_ORDER_URL, request, getByBitHeadersWithSign(request)) }

        val parsedJson = jsonPath.parse(response.bodyAsText())
        when (val returnCode = parsedJson.read<Int>(RET_CODE_KEY)) {
            0, 110017 -> return
            10002 -> closePosition(symbol, isLong, size, positionIdx, repeatCount)
            10004 -> throw InvalidSignatureException()
            33004 -> throw ApiTokenExpiredException()
            else -> throw UnknownRetCodeException(returnCode)
        }
    }

    @Synchronized
    private fun getByBitHeadersWithSign(body: String): Array<Pair<String, String>> {
        val timestamp = System.currentTimeMillis().toString()
        return arrayOf(
            "X-BAPI-API-KEY" to apiKey,
            "X-BAPI-SIGN" to encryptor.doFinal("$timestamp${apiKey}3000$body".toByteArray()).bytesToHex(),
            "X-BAPI-SIGN-TYPE" to "2",
            "X-BAPI-TIMESTAMP" to timestamp,
            "X-BAPI-RECV-WINDOW" to "3000",
            "Content-Type" to "application/json"
        )
    }
}
