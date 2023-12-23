package space.dawdawich.integration.client.bybit

import com.jayway.jsonpath.ParseContext
import space.dawdawich.integration.client.DefaultHttpClient
import space.dawdawich.integration.model.PairInfo
import io.ktor.client.*
import io.ktor.client.statement.*
import space.dawdawich.exception.UnknownRetCodeException

open class ByBitPublicHttpClient(serverUrl: String, client: HttpClient, val jsonPath: ParseContext) :
    DefaultHttpClient(serverUrl, client) {

    companion object {
        const val GET_INSTRUMENTS_INFO = "/market/instruments-info"
    }

    suspend fun getPairInstructions(symbol: String): PairInfo {
        val response = get(GET_INSTRUMENTS_INFO, "category=linear&symbol=$symbol")

        val parsedJson = jsonPath.parse(response.bodyAsText())
        when (val returnCode = parsedJson.read<Int>("\$.retCode")) {
            0 -> {
                val pairData = arrayOf(
                    "priceFilter.minPrice",
                    "priceFilter.maxPrice",
                    "priceFilter.tickSize",
                    "lotSizeFilter.minOrderQty",
                    "lotSizeFilter.maxOrderQty",
                    "lotSizeFilter.qtyStep"
                ).map { parsedJson.read<String>("\$.result.list[0].$it").toDouble() }.toTypedArray()
                return PairInfo(pairData)
            }

            else -> {
                throw UnknownRetCodeException(returnCode)
            }
        }
    }
}
