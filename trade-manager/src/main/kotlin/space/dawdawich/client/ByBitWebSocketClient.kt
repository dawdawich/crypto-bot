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
import java.lang.Exception
import java.net.URI
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration.Companion.hours

class ByBitWebSocketClient(
    private val apiKey: String,
    private val encryptor: Mac,
    private val positionSubscriber: (PositionResponse) -> Unit,
    private val orderSubscriber: (OrderResponse) -> Unit
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
                "args" to listOf(apiKey, signatureWithExpiration.second, signatureWithExpiration.first)
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
                if (topic == "position") {
                    val positionsToUpdate = response.read<List<PositionResponse>>("\$.data")
                    positionsToUpdate.forEach { position -> positionSubscriber(position) }
                } else if (topic == "order") {
                    val ordersToUpdate = response.read<List<OrderResponse>>("\$.data")
                    ordersToUpdate.forEach { orderResponse -> orderSubscriber(orderResponse) }
                }
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        GlobalScope.launch { reconnect() }
    }

    override fun onError(ex: Exception?) {
        println("Failed to listen websocket. $ex")
    }

    private fun getAuthData(): Pair<String, Long> {
        val expireTime = System.currentTimeMillis() + 2.hours.inWholeMilliseconds
        val signature =
            encryptor.doFinal("GET/realtime$expireTime".toByteArray()).joinToString { String.format("%02x", it) }

        return signature to expireTime
    }
}
