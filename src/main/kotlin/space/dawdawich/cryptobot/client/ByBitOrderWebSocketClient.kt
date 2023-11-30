package space.dawdawich.cryptobot.client

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import space.dawdawich.cryptobot.interfaces.OrderUpdateConsumer
import space.dawdawich.cryptobot.logger
import space.dawdawich.cryptobot.util.HttpUtils
import space.dawdawich.cryptobot.util.json
import space.dawdawich.cryptobot.util.jsonPath
import java.net.URI
import kotlin.Exception

class ByBitOrderWebSocketClient : WebSocketClient(URI("wss://stream.bybit.com/v5/private")) {
    val subscribers: MutableList<OrderUpdateConsumer> = mutableListOf()

    override fun onOpen(handshakedata: ServerHandshake?) {
        logger.info { "Connected to Bybit Order WebSocket." }
        val (key, expire, signature) = HttpUtils.getSighForWebsocket()
        send("{\"op\":\"auth\",\"args\":[\"$key\", $expire, \"$signature\"]}")

        // Subscribe to the desired topics after connecting
        send("{\"op\":\"subscribe\",\"args\":[\"order.linear\"]}")
    }

    override fun onMessage(message: String?) {
        logger.info { "received order message: $message" }
        message?.let { message ->
            val jsonResponse = jsonPath.parse(message)
            jsonResponse.read<Boolean?>("\$.success")?.let {
                if (!it) {
                    throw Exception("Failed to connect to web socket")
                }
                return
            }
            subscribers.forEach {
                val order = JSONObject(jsonResponse.read<Map<String, Any>>("\$.data[0]"))
                if (order.getString("orderLinkId").isNotBlank()) {
                    it.process(json.decodeFromString(order.toString()))
                }
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        logger.info { "Order WebSocket closed. Code: $code, Reason: $reason" }
        GlobalScope.launch { reconnect() }
    }

    override fun onError(ex: Exception?) {
        logger.error { "Order WebSocketerror: " + (ex?.message ?: "null") }
    }
}
