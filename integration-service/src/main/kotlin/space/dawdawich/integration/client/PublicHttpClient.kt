package space.dawdawich.integration.client

import io.ktor.client.plugins.*
import kotlinx.coroutines.runBlocking
import space.dawdawich.integration.model.PairInfo

interface PublicHttpClient {

//    suspend fun getPairCurrentPrice(symbol: String): Double
    suspend fun getPairCurrentPrice(): List<Map<String, String>>
    suspend fun getPairInstructions(symbol: String): PairInfo
    suspend fun getKLineClosePrices(symbol: String): List<Double>

    suspend infix fun <T> Int.repeatTry(block: suspend () -> T): T {
        return try {
            runBlocking { block() }
        } catch (timeoutEx: HttpRequestTimeoutException) {
            if (this > 0) {
                (this - 1).repeatTry(block)
            } else {
                throw timeoutEx
            }
        }
    }
}
