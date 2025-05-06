package space.dawdawich.client

import space.dawdawich.repositories.mongo.PriceTickRepository
import space.dawdawich.repositories.mongo.entity.PriceTickModel
import space.dawdawich.util.jsonPath

class ByBitPriceChangeCaptureClient(private val priceTickRepository: PriceTickRepository, connectionUrl: String) : AbstractWebSocketClient(connectionUrl) {
    override val socketTopicName: String = "tickers"

    public override fun addSubscription(symbol: String) = super.addSubscription(symbol)

    override fun onMessage(message: String?) {
        message?.let {
            with(jsonPath.parse(it)) {
                read<String?>("\$.data.lastPrice")?.let { checkedPrice ->
                    read<String?>("\$.data.symbol")?.let { symbol ->
                        priceTickRepository.insert(PriceTickModel(symbol.hashCode(), checkedPrice.toDouble(), System.currentTimeMillis()))
                    }
                }
            }
        }
    }
}
