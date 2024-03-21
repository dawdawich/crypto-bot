package space.dawdawich.integration.client

import io.ktor.client.plugins.*
import kotlinx.coroutines.runBlocking
import space.dawdawich.integration.model.PairInfo

interface PublicHttpClient {

    suspend fun getPairCurrentPrice(symbol: String): Double
    suspend fun getPairInstructions(symbol: String): PairInfo

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