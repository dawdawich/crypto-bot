package space.dawdawich.client

import space.dawdawich.constants.BYBIT_KLINE_TOPIC
import space.dawdawich.constants.BYBIT_TEST_KLINE_TOPIC
import space.dawdawich.model.analyzer.KLineRecord
import space.dawdawich.service.RabbitManager
import space.dawdawich.util.jsonPath

class BybitKLineWebSocketClient(private val rabbitManager: RabbitManager, connectionUrl: String, isDemo: Boolean) :
    AbstractWebSocketClient(connectionUrl) {
    private val topicName: String = if (isDemo) BYBIT_TEST_KLINE_TOPIC else BYBIT_KLINE_TOPIC
    override val socketTopicName: String = "kline"

    fun addSubscription(symbol: String, interval: Int) = when (interval) {
        1, 3, 5, 15, 30, 60, 120, 240, 360, 720 -> super.addSubscription("$interval.$symbol")
        else -> throw IllegalArgumentException("Invalid interval '$interval'. Possible interval: 1, 3, 5, 15, 30, 60, 120, 240, 360, 720")
    }

    override fun onMessage(message: String?) {
        message?.let { json ->
            with(jsonPath.parse(json)) {
                read<String?>("\$.topic")?.let { messageTopic ->
                    if (messageTopic.startsWith(socketTopicName)) {
                        val symbol = messageTopic.split(".")[2]

                        read<List<LinkedHashMap<String, String>>>("\$.data[?(@.confirm == true)]").forEach {
                            rabbitManager.sendKLineEvent(topicName, symbol, KLineRecord(
                                it["interval"]!!,
                                it["open"]!!.toDouble(),
                                it["close"]!!.toDouble(),
                                it["high"]!!.toDouble(),
                                it["low"]!!.toDouble(),
                            ))
                        }
                    }
                }
            }
        }
    }
}
