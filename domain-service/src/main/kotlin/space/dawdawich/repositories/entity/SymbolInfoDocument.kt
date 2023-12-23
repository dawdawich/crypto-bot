package space.dawdawich.repositories.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "symbols_to_listen")
@Serializable
data class SymbolInfoDocument(
    @Id
    @SerialName("_id")
    val symbol: String,
    @Indexed(unique = true)
    val partition: Int,
    val isOneWayMode: Boolean,
    val tickSize: Double,
    val minPrice: Double,
    val maxPrice: Double,
    val minOrderQty: Double,
    val maxOrderQty: Double,
    val qtyStep: Double
)
