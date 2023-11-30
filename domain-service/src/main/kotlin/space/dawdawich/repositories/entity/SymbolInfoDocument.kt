package space.dawdawich.repositories.entity

import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "symbols_to_listen")
data class SymbolInfoDocument(
    @Indexed(unique = true)
    val symbol: String,
    val partition: Int
)
