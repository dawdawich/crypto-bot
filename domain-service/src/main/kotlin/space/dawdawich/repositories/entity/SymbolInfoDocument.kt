package space.dawdawich.repositories.entity

import kotlinx.serialization.Serializable
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "symbols_to_listen")
@Serializable
data class SymbolInfoDocument(
    @Id
    val symbol: String,
    @Indexed(unique = true)
    val partition: Int,
    val isOneWayMode: Boolean,
    val priceMinStep: Double
)
