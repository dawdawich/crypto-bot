package dawdawich.space.client.bybit

import com.jayway.jsonpath.ParseContext
import dawdawich.space.client.DefaultHttpClient
import io.ktor.client.*
import io.ktor.client.statement.*
import space.dawdawich.exception.UnknownRetCodeException

open class ByBitPublicHttpClient(serverUrl: String, client: HttpClient, val jsonPath: ParseContext) :
    DefaultHttpClient(serverUrl, client) {

    companion object {
        const val GET_INSTRUMENTS_INFO = "/market/instruments-info"
    }

    suspend fun getPairInstructions(symbol: String): Pair<Double, Double> {
        val response = get(GET_INSTRUMENTS_INFO, "category=linear&symbol=$symbol")

        val parsedJson = jsonPath.parse(response.bodyAsText())
        when (val returnCode = parsedJson.read<Int>("\$.retCode")) {
            0 -> {
                return parsedJson.read<String>("\$.result.list[0].lotSizeFilter.qtyStep")
                    .toDouble() to parsedJson.read<String>("\$.result.list[0].priceFilter.minPrice").toDouble()
            }

            else -> {
                throw UnknownRetCodeException(returnCode)
            }
        }
    }
}
