package space.dawdawich.integration.client.bybit

import com.jayway.jsonpath.ParseContext
import space.dawdawich.integration.client.DefaultHttpClient
import space.dawdawich.integration.model.PairInfo
import io.ktor.client.*
import io.ktor.client.statement.*
import io.ktor.http.*
import space.dawdawich.exception.UnknownRetCodeException
import space.dawdawich.exception.UnsuccessfulOperationException
import space.dawdawich.integration.client.PublicHttpClient

open class ByBitPublicHttpClient(serverUrl: String, client: HttpClient, val jsonPath: ParseContext) :
    DefaultHttpClient(serverUrl, client), PublicHttpClient {

    companion object {
        const val GET_INSTRUMENTS_INFO = "/market/instruments-info"
        const val GET_TICKER = "/market/tickers"
    }

    override suspend fun getPairCurrentPrice(symbol: String): Double {
        val response = get(GET_TICKER, "category=linear&symbol=$symbol")

        if (response.status != HttpStatusCode.OK) {
            throw UnsuccessfulOperationException(response.status.value)
        }

        val parsedJson = jsonPath.parse(response.bodyAsText())
        when (val returnCode = parsedJson.read<Int>("\$.retCode")) {
            0 -> {
                return parsedJson.read<String>("\$.result.list[0].lastPrice").toDouble()
            }

            else -> {
                throw UnknownRetCodeException(returnCode)
            }
        }
    }

    override suspend fun getPairInstructions(symbol: String): PairInfo {
        val response = get(GET_INSTRUMENTS_INFO, "category=linear&symbol=$symbol")

        if (response.status != HttpStatusCode.OK) {
            throw UnsuccessfulOperationException(response.status.value)
        }

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
}
