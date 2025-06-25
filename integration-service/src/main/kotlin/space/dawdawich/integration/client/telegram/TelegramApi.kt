package space.dawdawich.integration.client.telegram

import io.ktor.client.*
import io.ktor.client.request.*

open class TelegramApiClient(private val client: HttpClient) {
    suspend fun sendMessage(apiToken: String, chatId: Long, message: String) {
        client.get("https://api.telegram.org/bot$apiToken/sendMessage?chat_id=$chatId&text=$message") {}
    }
}
