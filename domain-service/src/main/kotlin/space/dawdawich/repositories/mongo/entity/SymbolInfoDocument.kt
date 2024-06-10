package space.dawdawich.repositories.mongo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "symbols_to_listen")
data class SymbolInfoDocument(
    @Id
    val symbol: String,
    @Indexed(unique = true)
    val partition: Int,
    val tickSize: Double,
    val minPrice: Double,
    val maxPrice: Double,
    val minOrderQty: Double,
    val maxOrderQty: Double,
    val maxLeverage: Double,
    val qtyStep: Double,
    val volatilityCoef: Double? = null
)
