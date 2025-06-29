package space.dawdawich.integration.client

import io.ktor.client.plugins.*
import space.dawdawich.integration.model.PairInfo
import space.dawdawich.model.Market

interface PublicHttpClient {

    suspend fun getPairInstructions(symbol: String): PairInfo

    suspend fun getPairInstructionsWithCursor(cursor: String? = null): List<PairInfo>

    fun getMarket(): Market

    suspend infix fun <T> Int.repeatTry(block: suspend () -> T): T {
        return try {
            block()
        } catch (timeoutEx: HttpRequestTimeoutException) {
            if (this > 0) {
                (this - 1).repeatTry(block)
            } else {
                throw timeoutEx
            }
        }
    }
}
