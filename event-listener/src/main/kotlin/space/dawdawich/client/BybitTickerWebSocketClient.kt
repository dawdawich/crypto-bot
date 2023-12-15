package space.dawdawich.client

import kotlinx.coroutines.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.springframework.stereotype.Component
import space.dawdawich.service.KafkaManager
import space.dawdawich.util.jsonPath
import java.lang.Exception
import java.net.URI

@Component
class BybitTickerWebSocketClient(private val kafkaManager: KafkaManager) :
    WebSocketClient(URI("wss://stream.bybit.com/v5/public/linear")) {
    val mapSymbolsToPartition: MutableMap<String, Int> = mutableMapOf()

    fun addSubscription(symbol: String) {
        send("{\"op\":\"subscribe\",\"args\":[\"tickers.$symbol\"]}")
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        // Subscribe to the desired topics after connecting
        mapSymbolsToPartition.keys.forEach {
            send("{\"op\":\"subscribe\",\"args\":[\"tickers.$it\"]}")
        }
    }

    override fun onMessage(message: String?) {
        message?.let {
            val parsedMessage = jsonPath.parse(it)
            parsedMessage.read<String?>("\$.data.markPrice")?.let { checkedPrice ->
                parsedMessage.read<String?>("\$.data.symbol")?.let { symbol ->
                    kafkaManager.sendTickerEvent(symbol, mapSymbolsToPartition[symbol]!!, checkedPrice)
                }
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        if (remote) {
            runBlocking { launch { reconnect() } }
        }
    }

    override fun onError(ex: Exception?) {
        println(ex)
    }
}
