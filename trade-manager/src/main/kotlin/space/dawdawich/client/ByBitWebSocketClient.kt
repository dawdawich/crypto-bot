package space.dawdawich.client

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import space.dawdawich.client.responses.OrderResponse
import space.dawdawich.client.responses.PositionResponse
import space.dawdawich.service.TradeManager
import space.dawdawich.service.model.Order
import space.dawdawich.service.model.Position
import java.lang.Exception
import java.net.URI
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration.Companion.hours

class ByBitWebSocketClient(
    private val apiKey: String,
    private val encryptor: Mac,
    private val tradeManager: TradeManager
) : WebSocketClient(URI("wss://stream.bybit.com/v5/private")) {
    companion object {
        private val jsonPath =
            JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))
    }

    private var signatureWithExpiration: Pair<String, Long>

    init {
        signatureWithExpiration = getAuthData()
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        val operationRequest = JSONObject(
            mapOf(
                "op" to "auth",
                "args" to listOf(apiKey, signatureWithExpiration.second.toString(), signatureWithExpiration.first)
            )
        ).toString()
        send(operationRequest)
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
                    // can be logged this event
                }
            }
            response.read<String?>("\$.topic")?.let { topic ->
                if (topic == "position.linear") {
                    val positionsToUpdate = response.read<List<Map<String, Any>>>("\$.data")
                    positionsToUpdate.forEach { position ->
                        val toUpdate = if (position["side"].toString().equals("none", true)) {
                            listOf(
                                Position(
                                    position["symbol"].toString(),
                                    true,
                                    position["size"].toString().toDouble(),
                                    position["entryPrice"].toString().toDouble(),
                                    position["positionIdx"].toString().toInt(),
                                    position["updatedTime"].toString().toLong()
                                ),
                                Position(
                                    position["symbol"].toString(),
                                    false,
                                    position["size"].toString().toDouble(),
                                    position["entryPrice"].toString().toDouble(),
                                    position["positionIdx"].toString().toInt(),
                                    position["updatedTime"].toString().toLong()
                                )
                            )
                        } else {
                            listOf(Position(
                                position["symbol"].toString(),
                                position["side"].toString().equals("buy", true),
                                position["size"].toString().toDouble(),
                                position["entryPrice"].toString().toDouble(),
                                position["positionIdx"].toString().toInt(),
                                position["updatedTime"].toString().toLong()
                            ))
                        }
                        tradeManager.updatePosition(toUpdate)
                    }
                } else if (topic == "order.linear") {
                    val ordersToUpdate = response.read<List<Map<String, Any>>>("\$.data")
                    ordersToUpdate.forEach { orderResponse ->
                        tradeManager.updateOrder(
                            Order(
                                orderResponse["symbol"].toString(),
                                orderResponse["side"].toString().equals("buy", true),
                                orderResponse["price"].toString().toDouble(),
                                orderResponse["qty"].toString().toDouble(),
                                orderResponse["orderStatus"].toString(),
                                orderResponse["orderLinkId"].toString()
                            )
                        )
                    }
                }
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        signatureWithExpiration = getAuthData()
        GlobalScope.launch { reconnect() }
    }

    override fun onError(ex: Exception?) {
        println("Failed to listen websocket. $ex")
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
