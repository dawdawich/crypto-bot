package space.dawdawich.integration.client.telegram

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*

open class TelegramApiClient(private val client: HttpClient) {
    suspend fun sendMessage(apiToken: String, chatId: Long, message: String) {
        5.repeatTry { client.get("https://api.telegram.org/bot$apiToken/sendMessage?chat_id=$chatId&text=$message") {} }
    }

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
