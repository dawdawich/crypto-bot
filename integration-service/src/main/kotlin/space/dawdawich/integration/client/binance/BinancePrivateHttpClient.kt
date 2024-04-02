package space.dawdawich.integration.client.binance

import com.jayway.jsonpath.ParseContext
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.http.*
import mu.KotlinLogging
import space.dawdawich.exception.InsufficientBalanceException
import space.dawdawich.exception.InvalidSignatureException
import space.dawdawich.exception.UnknownRetCodeException
import space.dawdawich.exception.UnsuccessfulOperationException
import space.dawdawich.integration.client.PrivateHttpClient
import space.dawdawich.integration.client.bybit.RET_CODE_KEY
import space.dawdawich.utils.bytesToHex
import java.nio.channels.UnresolvedAddressException
import javax.crypto.Mac
import kotlin.jvm.Throws

private const val CODE_KEY = "\$.code"

class BinancePrivateHttpClient(
        serverUrl: String,
        client: HttpClient,
        jsonPath: ParseContext,
        private val apiKey: String,
        private val encryptor: Mac,
) : BinancePublicHttpClient(serverUrl, client, jsonPath), PrivateHttpClient {
    companion object {
        const val CREATE_ORDER_URL = "/api/v3/order"
//        const val GET_ACCOUNT_BALANCE = "/account/wallet-balance"
//        const val SET_MARGIN = "/position/set-leverage"
        const val CANCEL_ALL_ORDERS = "/api/v3/openOrders"
        const val CANCEL_ORDER = "/api/v3/order"
//        const val GET_POSITIONS = "/position/list"
    }

    private val logger = KotlinLogging.logger {}

    @Throws(HttpRequestTimeoutException::class)
    override suspend fun createOrder(
            symbol: String,
            entryPrice: Double,
            qty: Double,
            isLong: Boolean,
            orderId: String,
            positionIdx: Int,
            repeatCount: Int,
    ): Boolean {
        val queryParams = arrayOf(
                "symbol" to symbol,
                "side" to if (isLong) "BUY" else "SELL",
                "type" to "LIMIT",
                "timeInForce" to "GTC",
                "quantity" to qty.toString(),
                "price" to entryPrice.toString(),
                "newClientOrderId" to orderId,
                "timestamp" to System.currentTimeMillis().toString()
        )

        val headers = arrayOf(
                "X-MBX-APIKEY" to apiKey,
                "Content-Type" to "application/json"
        )

        try {
            val response = repeatCount repeatTry { post(CREATE_ORDER_URL, getQueryParamsWithSignature(queryParams), headers) }

            if (response.status == HttpStatusCode.OK) {
                return true
            } else {
                val parsedJson = jsonPath.parse(response.bodyAsText())
                when (val returnCode = parsedJson.read<Int>(CODE_KEY)) {
                    -1004 -> createOrder(symbol, entryPrice, qty, isLong, orderId, positionIdx, repeatCount)
                    -1101, -1102, -1103 -> {
                        logger.info { "Failed to create order. Reason:\n${parsedJson.jsonString()}" }
                        return false
                    }

                    -1022 -> throw InvalidSignatureException()
                    -3041, -5002 -> throw InsufficientBalanceException()
                    else -> throw UnknownRetCodeException(returnCode)
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to create order on binance server" }
        }
        return false
    }

    override suspend fun cancelAllOrder(symbol: String, repeatCount: Int) {
        val queryParams = arrayOf(
                "symbol" to symbol,
                "timestamp" to System.currentTimeMillis().toString()
        )

        val headers = arrayOf(
                "X-MBX-APIKEY" to apiKey,
                "Content-Type" to "application/json"
        )

        try {
            val response = repeatCount repeatTry { delete(CANCEL_ALL_ORDERS, getQueryParamsWithSignature(queryParams), headers) }
            if (response.status != HttpStatusCode.OK) {
                val parsedJson = jsonPath.parse(response.bodyAsText())
                when (val returnCode = parsedJson.read<Int>(CODE_KEY)) {
                    -1004 -> cancelAllOrder(symbol, repeatCount)
                    -1022 -> throw InvalidSignatureException()
                    else -> throw UnknownRetCodeException(returnCode)
                }
            }
        } catch (e: UnresolvedAddressException) {
            logger.warn { "Failed to cancel order" }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun cancelOrder(symbol: String, orderId: String, repeatCount: Int): Boolean {
        val queryParams = arrayOf(
                "symbol" to symbol,
                "orderId" to orderId,
                "timestamp" to System.currentTimeMillis().toString()
        )

        val headers = arrayOf(
                "X-MBX-APIKEY" to apiKey,
                "Content-Type" to "application/json"
        )

        try {
            val response = repeatCount repeatTry { delete(CANCEL_ORDER, getQueryParamsWithSignature(queryParams), headers) }
            if (response.status == HttpStatusCode.OK) {
                return true
            } else {
                val parsedJson = jsonPath.parse(response.bodyAsText())
                when (val returnCode = parsedJson.read<Int>(CODE_KEY)) {
                    -1004 -> cancelOrder(symbol, orderId, repeatCount)
                    -1022 -> throw InvalidSignatureException()
                    else -> throw UnknownRetCodeException(returnCode)
                }
            }
        } catch (e: UnresolvedAddressException) {
            logger.warn { "Failed to cancel order" }
        } catch (e: Exception) {
            throw e
        }
        return false
    }
//
//    override suspend fun getAccountBalance(repeatCount: Int): Double {
//        val query = "accountType=UNIFIED&coin=USDT"
//        val response: HttpResponse =
//            repeatCount repeatTry { get(GET_ACCOUNT_BALANCE, query, getBinanceHeaders(query)) }
//        val parsedJson = jsonPath.parse(response.bodyAsText())
//
//        return when (val returnCode = parsedJson.read<Int>(RET_CODE_KEY)) {
//            0 -> parsedJson.read<String>("$.result.list[0].coin[0].equity").toDouble()
//            10002 -> getAccountBalance(repeatCount)
//            10004 -> throw InvalidSignatureException()
//            else -> throw UnknownRetCodeException(returnCode)
//        }
//    }
//
//
//    override suspend fun getPositionInfo(symbol: String, retryCount: Int): List<PositionInfo> {
//        val query = "category=linear&symbol=$symbol"
//        val headers = getBinanceHeaders(query)
//        val response = retryCount repeatTry { get(GET_POSITIONS, query, headers) }
//        val parsedJson = jsonPath.parse(response.bodyAsText())
//
//        return when (val returnCode = parsedJson.read<Int>(RET_CODE_KEY)) {
//            0 -> parsedJson.read<List<Map<String, Any>>>("\$.result.list").map { position ->
//                PositionInfo(
//                        position["symbol"].toString(),
//                        position["side"].toString().equals("buy", true),
//                        position["size"].toString().toDouble(),
//                        position["positionValue"].toString().let { if (it.isBlank()) 0.0 else it.toDouble() },
//                        position["positionIdx"].toString().toInt(),
//                        position["updatedTime"].toString().let { if (it.isBlank()) 0 else it.toLong() }
//                )
//            }
//
//            10002 -> getPositionInfo(symbol, retryCount)
//            10004 -> throw InvalidSignatureException()
//            else -> throw UnknownRetCodeException(returnCode)
//        }
//    }
//
//    override suspend fun setMarginMultiplier(symbol: String, multiplier: Int, retryCount: Int) {
//        val request: String = buildJsonObject {
//            put("category", "linear")
//            put("symbol", symbol)
//            put("buyLeverage", multiplier.toString())
//            put("sellLeverage", multiplier.toString())
//        }.toString()
//        val response: HttpResponse =
//            retryCount repeatTry { post(SET_MARGIN, request, getBinanceHeaders(request)) }
//
//        val parsedJson = jsonPath.parse(response.bodyAsText())
//        when (val returnCode = parsedJson.read<Int>(RET_CODE_KEY)) {
//            0, 110043 -> return
//            10001 -> throw InvalidMarginProvided()
//            10002 -> setMarginMultiplier(symbol, multiplier, retryCount)
//            10004 -> throw InvalidSignatureException()
//            else -> throw UnknownRetCodeException(returnCode)
//        }
//    }
//
//    override suspend fun closePosition(
//        symbol: String,
//        isLong: Boolean,
//        size: Double,
//        positionIdx: Int,
//        repeatCount: Int,
//    ) {
//        val request: String = buildJsonObject {
//            put("category", "linear")
//            put("symbol", symbol)
//            put("side", if (isLong) "Sell" else "Buy")
//            put("qty", size.toString())
//            put("positionIdx", positionIdx.toString())
//            put("isLeverage", 1)
//            put("reduceOnly", true)
//            put("orderType", "Market")
//            put("timeInForce", "IOC")
//        }.toString()
//
//        val response: HttpResponse =
//            repeatCount repeatTry { post(CREATE_ORDER_URL, request, getBinanceHeaders(request)) }
//
//        val parsedJson = jsonPath.parse(response.bodyAsText())
//        when (val returnCode = parsedJson.read<Int>(RET_CODE_KEY)) {
//            0, 110017 -> return
//            10002 -> closePosition(symbol, isLong, size, positionIdx, repeatCount)
//            10004 -> throw InvalidSignatureException()
//            else -> {
//                throw UnknownRetCodeException(returnCode)
//            }
//        }
//    }

    @Synchronized
    private fun getQueryParamsWithSignature(queryParams: Array<Pair<String, String>>): Array<Pair<String, String>> {
        val joinedQueryParams = queryParams.joinToString("&") { "${it.first}=${it.second}" }
        val signature = encryptor.doFinal(joinedQueryParams.toByteArray()).bytesToHex()
        return queryParams + Pair("signature", signature)
    }
}