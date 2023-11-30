package space.dawdawich.repositories.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("bybit_access_tokens")
data class ByBitApiTokens(
    @Id
    val id: String,
    val apiKey: String,
    val secretKey: String
) {
}
