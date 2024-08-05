package space.dawdawich.integration.client.bybit

import com.jayway.jsonpath.ParseContext
import space.dawdawich.integration.client.DefaultHttpClient
import space.dawdawich.integration.model.PairInfo
import io.ktor.client.*
import io.ktor.client.statement.*
import space.dawdawich.exception.UnknownRetCodeException
import space.dawdawich.integration.client.PublicHttpClient
import kotlin.time.Duration.Companion.days

open class ByBitPublicHttpClient(serverUrl: String, client: HttpClient, val jsonPath: ParseContext) :
    DefaultHttpClient(serverUrl, client), PublicHttpClient {

    companion object {
        const val GET_INSTRUMENTS_INFO = "/market/instruments-info"
        const val GET_TICKER = "/market/tickers"
        const val GET_KLINE = "/market/kline"
    }

    override suspend fun getPairCurrentPrice(): List<Map<String, String>> {
//        val response = 5 repeatTry { get(GET_TICKER, "category=linear&symbol=$symbol") }
        val response = 5 repeatTry { get(GET_TICKER, "category=linear") }

        val parsedJson = jsonPath.parse(response.bodyAsText())
        when (val returnCode = parsedJson.read<Int>("\$.retCode")) {
            0 -> {
                return parsedJson.read("\$.result.list")
//                return parsedJson.read<String>("\$.result.list[0].lastPrice").toDouble()
            }

            else -> {
                throw UnknownRetCodeException(returnCode)
            }
        }
    }

    override suspend fun getPairInstructions(symbol: String): PairInfo {
        val response = 5 repeatTry { get(GET_INSTRUMENTS_INFO, "category=linear&symbol=$symbol") }

        val parsedJson = jsonPath.parse(response.bodyAsText())
        when (val returnCode = parsedJson.read<Int>("\$.retCode")) {
            0 -> {
                val pairData = arrayOf(
                    "priceFilter.minPrice",
                    "priceFilter.maxPrice",
                    "priceFilter.tickSize",
                    "lotSizeFilter.minOrderQty",
                    "lotSizeFilter.maxOrderQty",
                    "lotSizeFilter.qtyStep",
                    "leverageFilter.maxLeverage"
                ).map { parsedJson.read<String>("\$.result.list[0].$it").toDouble() }.toTypedArray()
                return PairInfo(pairData)
            }

            else -> {
                throw UnknownRetCodeException(returnCode)
            }
        }
    }

    override suspend fun getKLineClosePrices(symbol: String, interval: Int): List<Double> {
        val queryString =
            "category=linear&symbol=$symbol&interval=$interval&limit=1000"
        val response = 6 repeatTry { get(GET_KLINE, queryString) }

        val parsedJson = jsonPath.parse(response.bodyAsText())
        when (val returnCode = parsedJson.read<Int>("\$.retCode")) {
            0 -> {
                return parsedJson.read<List<List<String>>>("\$.result.list[*]").sortedBy { it[0].toDouble() }
                    .map { it[4].toDouble() }.toMutableList().apply { if (isNotEmpty())removeLast() }
            }

            else -> throw UnknownRetCodeException(returnCode, queryString)
        }
    }
}
