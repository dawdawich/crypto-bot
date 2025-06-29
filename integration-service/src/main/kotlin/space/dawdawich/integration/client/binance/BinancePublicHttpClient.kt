package space.dawdawich.integration.client.binance

import com.jayway.jsonpath.ParseContext
import io.ktor.client.*
import io.ktor.client.statement.*
import space.dawdawich.integration.client.DefaultHttpClient
import space.dawdawich.integration.client.PublicHttpClient
import space.dawdawich.integration.model.PairInfo
import space.dawdawich.model.Market

class BinancePublicHttpClient(serverUrl: String, client: HttpClient, private val jsonPath: ParseContext) :
    DefaultHttpClient(serverUrl, client), PublicHttpClient {

    companion object {
        const val GET_INSTRUMENTS_INFO = "/fapi/v1/exchangeInfo"
    }

    override suspend fun getPairInstructions(symbol: String): PairInfo {
        TODO("Not yet implemented")
    }

    override suspend fun getPairInstructionsWithCursor(cursor: String?): List<PairInfo> {
        val response = 5 repeatTry { get(GET_INSTRUMENTS_INFO) }

        val parsedJson = jsonPath.parse(response.bodyAsText())

        val resultDataSize = parsedJson.read<Int>("\$.symbols.length()")
        val pairInfoResult = mutableListOf<PairInfo>()

        for (i in 0 until resultDataSize) {
            if (parsedJson.read<String>("\$.symbols[$i].contractType") != "PERPETUAL" || parsedJson.read<String>("\$.symbols[$i].status") != "TRADING") continue
            var minPrice = 0.0
            var maxPrice = 0.0
            var minQty = 0.0
            var maxQty = 0.0
            var qtyStep = 0.0

            for (j in 0..7) {
                val filterType = parsedJson.read<String>("\$.symbols[$i].filters[$j].filterType")
                if (filterType == "PRICE_FILTER") {
                    minPrice = parsedJson.read<String>("\$.symbols[$i].filters[$j].minPrice").toDouble()
                    maxPrice = parsedJson.read<String>("\$.symbols[$i].filters[$j].maxPrice").toDouble()
                }

                if (filterType == "LOT_SIZE") {
                    minQty = parsedJson.read<String>("\$.symbols[$i].filters[$j].minQty").toDouble()
                    maxQty = parsedJson.read<String>("\$.symbols[$i].filters[$j].maxQty").toDouble()
                    qtyStep = parsedJson.read<String>("\$.symbols[$i].filters[$j].stepSize").toDouble()
                }
            }

            pairInfoResult += PairInfo(
                minPrice,
                maxPrice,
                minQty,
                maxQty,
                -1.0,
                -1.0,
                qtyStep,
                parsedJson.read("\$.symbols[$i].onboardDate"),
                parsedJson.read("\$.symbols[$i].symbol"),
                Market.BINANCE
            )

        }
        return pairInfoResult
    }

    override fun getMarket() = Market.BINANCE
}
