package space.dawdawich.client

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

abstract class AbstractWebSocketClient(connectionUrl: String) :
    WebSocketClient(URI(connectionUrl)) {
    private val logger = KotlinLogging.logger {}
    abstract val socketTopicName: String

    val symbols: MutableList<String> = mutableListOf()

    protected open fun addSubscription(symbol: String) {
        symbols.add(symbol)
        send("{\"op\":\"subscribe\",\"args\":[\"$socketTopicName.$symbol\"]}")
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        // Subscribe to the desired topics after connecting
        symbols.forEach {
            send("{\"op\":\"subscribe\",\"args\":[\"$socketTopicName.$it\"]}")
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        logger.warn { "Websocket connection closed. Code: '$code'; Reason: '$reason'; Remote: '$remote'" }
        if (remote) {
            GlobalScope.launch { reconnect() }
        }
    }

    override fun onError(ex: Exception?) {
        logger.error(ex) { "Websocket got an error." }
    }
}
