package space.dawdawich.cryptobot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import space.dawdawich.cryptobot.service.response.PositionData
import space.dawdawich.cryptobot.util.HttpUtils
import space.dawdawich.cryptobot.util.json
import space.dawdawich.cryptobot.util.jsonPath

val logger = KotlinLogging.logger {}

object OrderManagerService {
    private const val BYBIT_URL_CREATE_ORDER = "https://api.bybit.com/v5/order/create"
    private const val BYBIT_URL_BATCH_CREATE_ORDER = "https://api.bybit.com/v5/order/create-batch"
    private const val BYBIT_URL_SET_MARGIN = "https://api.bybit.com/v5/position/set-leverage"
    private const val BYBIT_URL_GET_POSITION = "https://api.bybit.com/v5/position/list"
    private const val BYBIT_URL_GET_ORDER_REALTIME = "https://api.bybit.com/v5/order/realtime"
    private const val BYBIT_URL_INSTRUMENTS = "https://api.bybit.com/v5/market/instruments-info"
    private const val BYBIT_URL_BALANCE = "https://api.bybit.com/v5/account/wallet-balance"
    private const val BYBIT_URL_CANCEL_ALL = "https://api.bybit.com/v5/order/cancel-all"

    suspend fun getMinQty(pair: String): Pair<Double, Double> {
        val responseBody = HttpUtils.get("$BYBIT_URL_INSTRUMENTS?category=linear&symbol=$pair")

        logger.info { "Instrument Description" }
        logger.info { responseBody }

        return jsonPath.parse(responseBody).read<String>("\$.result.list[0].lotSizeFilter.qtyStep").toDouble() to jsonPath.parse(responseBody).read<String>("\$.result.list[0].priceFilter.minPrice").toDouble()
    }

    suspend fun createOrder(body: String): String? {
        val responseBody = HttpUtils.post(BYBIT_URL_CREATE_ORDER, body)

        logger.info { "Create Order Response" }
        logger.info { responseBody }

        return jsonPath.parse(responseBody).read<String?>("\$.result.orderLinkId")
    }

    suspend fun createOrders(body: String): List<String> {
        val responseBody = HttpUtils.post(BYBIT_URL_BATCH_CREATE_ORDER, body)

        logger.info { "Create Order Response" }
        logger.info { responseBody }

        val result = mutableListOf<String>()
        val parsedJson = jsonPath.parse(responseBody)
        val length = parsedJson.read<Int>("\$.retExtInfo.list.length()")

        for (i in 0..<length) {
            if (parsedJson.read<Int>("\$.retExtInfo.list[$i].code") == 0) {
                result += parsedJson.read<String>("\$.result.list[$i].orderLinkId")
            }
        }

        return result
    }

    suspend fun setMarginMultiplier(symbol: String, multiplier: Int): Boolean {
        val body = Json.encodeToString(
            mapOf(
                "category" to "linear",
                "symbol" to symbol,
                "buyLeverage" to multiplier.toString(),
                "sellLeverage" to multiplier.toString()
            )
        )

        val responseBody = HttpUtils.post(BYBIT_URL_SET_MARGIN, body)

        logger.info { "Set Margin Response" }
        logger.info { responseBody }

        jsonPath.parse(responseBody).read<Int>("\$.retCode")?.let {
            return it == 0 || it == 110043
        } ?: run { return false }
    }

    suspend fun hasNoActivePositions(pair: String, trend: String): Pair<Double, Double> {
        val responseBody = HttpUtils.get("$BYBIT_URL_GET_POSITION?category=linear&symbol=$pair")

        logger.info { "Has Active Position Description" }
        logger.info { responseBody }

        val data = jsonPath.parse(responseBody)
        val dataLength = data.read<Int>("\$.result.list.length()")
        if (dataLength == 1) {
            return (data.read<String?>("\$.result.list[0].size")?.toDouble() ?: 0.0) to (data.read<String?>("\$.result.list[0].unrealisedPnl")?.toDouble() ?: 0.0)
        }
        return if (data.read<String?>("\$.result.list[0].side") == trend) {
            (data.read<String?>("\$.result.list[0].size")?.toDouble() ?: 0.0) to (data.read<String?>("\$.result.list[0].unrealisedPnl")?.toDouble() ?: 0.0)
        } else {
            (data.read<String?>("\$.result.list[1].size")?.toDouble() ?: 0.0) to (data.read<String?>("\$.result.list[1].unrealisedPnl")?.toDouble() ?: 0.0)
        }
    }

    suspend fun getActivePositionInfo(pair: String): List<PositionData> {
        val responseBody = HttpUtils.get("$BYBIT_URL_GET_POSITION?category=linear&symbol=$pair")

        logger.info {  "Position Description" }
        logger.info {  responseBody }

        val data = jsonPath.parse(responseBody)

        val dataLength = data.read<Int>("\$.result.list.length()")
        if (dataLength == 1) {
            return listOf(json.decodeFromString(JSONObject(data.read<Map<String, Any>>("\$.result.list[0]")).toString()))
        }
        return json.decodeFromString(JSONArray(data.read<Map<String, Any>>("\$.result.list")).toString())
    }

    suspend fun isOrderDone(orderId: String): Boolean {
        val responseBody = HttpUtils.get("$BYBIT_URL_GET_ORDER_REALTIME?category=linear&orderId=$orderId")

        logger.info { "Is Order Filled Description" }
        logger.info { responseBody }

        val status = jsonPath.parse(responseBody).read<String?>("\$.result.list[0].orderStatus")
        return status?.let {
            return it == "Filled" || it == "Cancelled" || it == "Rejected" || it == "Deactivated"
        } ?: false
    }

    suspend fun getAccountBalance(): Double {
        val responseBody = HttpUtils.get("$BYBIT_URL_BALANCE?accountType=CONTRACT&coin=USDT")

        logger.info { "Account Balance Description" }
        logger.info { responseBody }

        return jsonPath.parse(responseBody).read<String?>("\$.result.list[0].coin[0].walletBalance")?.toDouble() ?: -1.0
    }

    suspend fun cancelAllOrders(pair: String): Boolean {
        val responseBody = HttpUtils.post(BYBIT_URL_CANCEL_ALL, Json.encodeToString(mapOf("category" to "linear", "symbol" to pair)))

        logger.info { "Cancel All Order Response" }
        logger.info { responseBody }

        return jsonPath.parse(responseBody).read<Int?>("\$.retCode") == 0
    }
}

