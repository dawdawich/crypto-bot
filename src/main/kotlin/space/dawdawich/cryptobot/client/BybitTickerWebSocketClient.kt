package space.dawdawich.cryptobot.client

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import space.dawdawich.cryptobot.analyzer.GridTableAnalyzer
import space.dawdawich.cryptobot.interfaces.AnalyzerInterface
import space.dawdawich.cryptobot.logger
import space.dawdawich.cryptobot.util.jsonPath
import java.lang.Exception
import java.net.URI
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

class BybitTickerWebSocketClient(private val pairs: List<String>) :
    WebSocketClient(URI("wss://stream.bybit.com/v5/public/linear")) {
        private val subsToRemove = CopyOnWriteArrayList<Pair<String, Consumer<Double>>>()

    companion object {
        lateinit var instance: BybitTickerWebSocketClient
    }

    init {
        instance = this
    }

    private val subscribersMap: MutableMap<String, MutableList<Consumer<Double>>> = Collections.synchronizedMap(
        mutableMapOf()
    )

    override fun onOpen(handshakedata: ServerHandshake?) {
        logger.info { "Connected to Bybit Ticker WebSocket. Pair - $pairs" }

        pairs.forEach {
            // Subscribe to the desired topics after connecting
            send("{\"op\":\"subscribe\",\"args\":[\"tickers.$it\"]}")
        }
    }

    override fun onMessage(message: String?) {
        message?.let {
            val price: Double? = jsonPath.parse(it).read<String?>("\$.data.markPrice")?.toDouble()

            price?.let { checkedPrice ->
                jsonPath.parse(it).read<String?>("\$.data.symbol")?.let { symbol ->
                    subscribersMap[symbol]?.parallelStream()?.forEach {
                        it.accept(checkedPrice)
                    }
                }
            }
        }

        subsToRemove.forEach {
            subscribersMap[it.first]!!.remove(it.second)
        }
        subsToRemove.clear()
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        logger.info { "Ticker WebSocket closed. Code: $code, Reason: $reason, Pair: $pairs" }
        GlobalScope.launch { reconnect() }
    }

    override fun onError(ex: Exception?) {
        logger.error { "Ticker WebSocketerror: " + (ex?.message ?: "null") }
    }

    @Synchronized
    fun addSubscriber(pair: String, subscriber: Consumer<Double>) {
        subscribersMap.getOrPut(pair) { mutableListOf() }.add(subscriber)
    }

    fun getSubscribers(): List<GridTableAnalyzer> {
        return subscribersMap.values.flatten().toSet().filterIsInstance<GridTableAnalyzer>()
    }

    fun removeSubscriber(pair: String, subscriber: Consumer<Double>) {
        subsToRemove += pair to subscriber
    }
}
