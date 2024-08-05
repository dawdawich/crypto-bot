package space.dawdawich.client

import space.dawdawich.constants.BYBIT_TEST_TICKER_TOPIC
import space.dawdawich.constants.BYBIT_TICKER_TOPIC
import space.dawdawich.service.RabbitManager
import space.dawdawich.util.jsonPath

class BybitTickerWebSocketClient(private val rabbitManager: RabbitManager, connectionUrl: String, isDemo: Boolean) :
    AbstractWebSocketClient(connectionUrl) {
    private val topicName: String = if (isDemo) BYBIT_TEST_TICKER_TOPIC else BYBIT_TICKER_TOPIC

    override val socketTopicName: String = "tickers"

    public override fun addSubscription(symbol: String) = super.addSubscription(symbol)

    override fun onMessage(message: String?) {
        message?.let {
            with(jsonPath.parse(it)) {
                read<String?>("\$.data.lastPrice")?.let { checkedPrice ->
                    read<String?>("\$.data.symbol")?.let { symbol ->
                        rabbitManager.sendTickerEvent(topicName, symbol, checkedPrice.toDouble())
                        rsiIndicators[symbol to "5"]?.let { rsi ->
                            rabbitManager.sendKLineEvent("tickerWithRSI", symbol, "$checkedPrice&${rsi.calculateRSI(checkedPrice.toDouble())}")
                        }
                    }
                }
            }
        }
    }
}
