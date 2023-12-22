package space.dawdawich.repositories.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import space.dawdawich.repositories.entity.constants.Market

@Document("api_token")
data class ApiAccessTokenDocument(
    @Id
    val id: String,
    val accountId: String,
    val apiKey: String,
    val secretKey: String,
    val market: Market,
    val test: Boolean
)