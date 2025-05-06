package space.dawdawich.integration.client.bybit

import com.jayway.jsonpath.ParseContext
import space.dawdawich.integration.client.DefaultHttpClient
import space.dawdawich.integration.model.PairInfo
import io.ktor.client.*
import io.ktor.client.statement.*
import space.dawdawich.exception.UnknownRetCodeException
import space.dawdawich.integration.client.PublicHttpClient

open class ByBitPublicHttpClient(serverUrl: String, client: HttpClient, private val jsonPath: ParseContext) :
    DefaultHttpClient(serverUrl, client), PublicHttpClient {

    companion object {
        const val GET_INSTRUMENTS_INFO = "/market/instruments-info"
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

}
