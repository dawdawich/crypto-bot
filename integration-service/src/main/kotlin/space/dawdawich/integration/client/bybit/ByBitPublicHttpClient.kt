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
                    "lotSizeFilter.minOrderQty",
                    "lotSizeFilter.maxOrderQty",
                    "leverageFilter.leverageStep",
                    "leverageFilter.maxLeverage",
                    "lotSizeFilter.qtyStep",
                    "launchTime"
                ).map { parsedJson.read<String>("\$.result.list[0].$it").toDouble() }.toTypedArray()
                return PairInfo(pairData, parsedJson.read("\$.result.list[0].symbol"))
            }

            else -> {
                throw UnknownRetCodeException(returnCode)
            }
        }
    }

    override suspend fun getPairInstructionsWithCursor(cursor: String?): List<PairInfo> {
        val response = 5 repeatTry { get(GET_INSTRUMENTS_INFO, "category=linear${cursor?.let { "&cursor=$it" }}") }

        val parsedJson = jsonPath.parse(response.bodyAsText())
        when (val returnCode = parsedJson.read<Int>("\$.retCode")) {
            0 -> {
                val resultDataSize = parsedJson.read<Int>("\$.result.list[*].size()")
                val pairInfoResult = mutableListOf<PairInfo>()
                for (i in 0 until resultDataSize) {
                    if (parsedJson.read("\$.result.list[$i].isPreListing")) continue

                    val pairData = arrayOf(
                        "priceFilter.minPrice",
                        "priceFilter.maxPrice",
                        "lotSizeFilter.minOrderQty",
                        "lotSizeFilter.maxOrderQty",
                        "leverageFilter.leverageStep",
                        "leverageFilter.maxLeverage",
                        "lotSizeFilter.qtyStep",
                        "launchTime"
                    ).map { parsedJson.read<String>("\$.result.list[$i].$it").toDouble() }.toTypedArray()

                    pairInfoResult += PairInfo(pairData, parsedJson.read("\$.result.list[$i].symbol"))
                }

                val newCursor = parsedJson.read<String?>("\$.result.nextPageCursor")

                if (newCursor != null && newCursor.isNotEmpty() && newCursor != cursor) {
                    pairInfoResult += getPairInstructions(newCursor)
                }

                return pairInfoResult
            }

            else -> {
                throw UnknownRetCodeException(returnCode)
            }
        }
    }

}
