package space.dawdawich.client

import com.jayway.jsonpath.ParseContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging.logger
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import space.dawdawich.client.model.FillOrderCallback
import space.dawdawich.client.model.PositionUpdateCallback
import space.dawdawich.exceptions.UnsupportedConfigurationException
import space.dawdawich.strategy.model.Position
import space.dawdawich.strategy.model.Trend
import java.net.URI
import javax.crypto.Mac
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class ByBitWebSocketClient(
    isTest: Boolean,
    private val apiKey: String,
    private val encryptor: Mac,
    private val jsonPath: ParseContext,
) : WebSocketClient(URI(if (isTest) BYBIT_TEST_SERVER_URL else BYBIT_SERVER_URL)) {

    var positionUpdateCallback: PositionUpdateCallback? = null
    var fillOrderCallback: FillOrderCallback? = null

    companion object {
        const val BYBIT_SERVER_URL = "wss://stream.bybit.com/v5/private"
        const val BYBIT_TEST_SERVER_URL = "wss://stream-testnet.bybit.com/v5/private"
    }

    private val logger = logger {}

    override fun onOpen(handshakedata: ServerHandshake?) {
        subscribe()
    }

    override fun onMessage(message: String?) {
        message?.let {
            val response = jsonPath.parse(it)
            response.read<String?>("\$.op")?.let { operation ->
                if (operation == "auth" && response.read("\$.success")) {
                    val operationRequest = JSONObject(
                        mapOf(
                            "op" to "subscribe",
                            "args" to listOf(
                                "position.linear",
                                "order.linear"
                            )
                        )
                    ).toString()
                    send(operationRequest)
                } else if (operation == "subscribe") {
                    logger.info { "Successfully subscribed to the topic." }
                }
            }

            if (positionUpdateCallback != null || fillOrderCallback != null) {
                response.read<String?>("\$.topic")?.let { topic ->
                    if (topic == "position.linear") {
                        positionUpdateCallback?.let { checkedPositionUpdateCallback ->
                            val positionsToUpdate = response.read<List<Map<String, Any>>>("\$.data")
                                .let { list ->
                                    if (list.size > 1) {
                                        throw UnsupportedConfigurationException("User's account enabled in hedge mode.")
                                    }
                                    list[0]
                                }
                                .let { position ->
                                    val side = position["side"].toString()
                                    val curRealizedPnL = position["curRealisedPnl"].toString().toDouble()
                                    if (side.isNotBlank()) {
                                        val trend = Trend.fromDirection(side)
                                        Position(
                                            position["entryPrice"].toString().toDouble(),
                                            position["size"].toString().toDouble(),
                                            trend,
                                            curRealizedPnL
                                        )
                                    } else null
                                }
                            logger.info { "Get position data to update: $positionsToUpdate" }
                            checkedPositionUpdateCallback.invoke(positionsToUpdate)
                        }
                    } else if (topic == "order.linear") {
                        fillOrderCallback?.let { checkedFillOrderCallback ->
                            response.read<List<Map<String, Any>>>("\$.data")
                                .map { orderResponse ->
                                    val id = orderResponse["orderLinkId"].toString()
                                    val status = orderResponse["orderStatus"].toString()
                                    id to status
                                }
                                .filter { order ->
                                    logger.info { "Obtain order to process. id: '${order.first}'; status: ${order.second}" }
                                    order.first.isNotBlank() && when (order.second.lowercase()) {
                                        "filled", "deactivated", "rejected", "cancelled" -> true
                                        else -> false
                                    }
                                }
                                .forEach { order ->
                                    logger.info { "Get order data to update: '${order.first}'; filled: '${order.second}'" }
                                    checkedFillOrderCallback.invoke(order.first)
                                }
                        }
                    }
                    response
                }
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        logger.info { "Web socket closed. Reason Code: '$code'; Reason: '$reason'; Remote: '$remote'" }
        if (code == 1006) {
            logger.info { "Reconnect web socket." }
            GlobalScope.launch { reconnect() }
        }
    }

    override fun onError(ex: Exception?) {
        logger.error(ex) { "Failed to listen websocket" }
        if (ex is java.net.UnknownHostException) {
            GlobalScope.launch {
                delay(1.minutes)
                reconnect()
            }
        }
    }

    private fun subscribe() {
        val signatureWithExpiration: Pair<String, Long> = getAuthData()
        val operationRequest = JSONObject(
            mapOf(
                "op" to "auth",
                "args" to listOf(apiKey, signatureWithExpiration.second.toString(), signatureWithExpiration.first)
            )
        ).toString()
        send(operationRequest)
    }

    private fun getAuthData(): Pair<String, Long> {
        val expireTime = System.currentTimeMillis() + 2.hours.inWholeMilliseconds
        val signature =
            encryptor.doFinal("GET/realtime$expireTime".toByteArray())
        var result = ""
        for (byte in signature) {
            result += String.format("%02x", byte)
        }

        return result to expireTime
    }
}
