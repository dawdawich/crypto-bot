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
                val result = parsedJson.read<Map<String, Any>>("\$.result.list[0]")
                return PairInfo(
                    result["minPrice"].toString().toDouble(),
                    result["maxPrice"].toString().toDouble(),
                    result["tickSize"].toString().toDouble(),
                    result["minOrderQty"].toString().toDouble(),
                    result["maxOrderQty"].toString().toDouble(),
                    result["qtyStep"].toString().toDouble(),
                )
            }

            else -> {
                throw UnknownRetCodeException(returnCode)
            }
        }
    }
}
