package space.dawdawich.cryptobot.client

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import space.dawdawich.cryptobot.analyzer.VoltyExpanCloseStrategyAnalyzer
import space.dawdawich.cryptobot.client.data.KLineResponse
import space.dawdawich.cryptobot.data.KLineIntervals
import space.dawdawich.cryptobot.util.json
import java.net.URI

class ByBitKLineWebSocketClient(private val pairs: List<String>, private val klineIntervals: List<KLineIntervals>) :
    WebSocketClient(URI("wss://stream.bybit.com/v5/public/linear")) {

    private val subscribersMap: MutableMap<String, MutableList<VoltyExpanCloseStrategyAnalyzer>> = mutableMapOf()

    override fun onOpen(handshakedata: ServerHandshake?) {
        println("Connected to Bybit KLine WebSocket. Pair - $pairs, KLine - $klineIntervals")
        // Subscribe to the desired topics after connecting
        pairs.forEach { pair ->
            klineIntervals.forEach { interval ->
                send("{\"op\":\"subscribe\",\"args\":[\"kline.$interval.$pair\"]}")
            }
        }
    }

    override fun onMessage(message: String?) {
        try {
            if (message?.contains("success") == true) {
                return
            }
            message?.let {
                json.decodeFromString<KLineResponse?>(it)?.let {
                    val pair = it.topic.split(".")[2]

                    it.data.filter { it.confirm }.forEach { data ->
                        subscribersMap[pair]?.parallelStream()?.filter { it.candleInterval.toString() == data.interval }
                            ?.forEach {
                                it.addStatistic(
                                    data.high.toDouble(),
                                    data.low.toDouble(),
                                    data.close.toDouble()
                                )
                            }
                    }
                }
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        println("KLine WebSocket closed. Code: $code, Reason: $reason, Pair: $pairs")
        GlobalScope.launch { reconnect() }
    }

    override fun onError(ex: Exception?) {
        System.err.println("KLine WebSocket(pair - $pairs) error: " + (ex?.message ?: "null"))
    }

    @Synchronized
    fun addSubscriber(pair: String, subscriber: VoltyExpanCloseStrategyAnalyzer) {
        subscribersMap.getOrPut(pair) { mutableListOf() }.add(subscriber)
    }

    fun getSubscribers(): Set<VoltyExpanCloseStrategyAnalyzer> {
        return subscribersMap.values.flatten().toSet()
    }
}
