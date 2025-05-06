package space.dawdawich.repositories.mongo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.io.Serializable

@Document(collection = "symbol")
data class SymbolDocument(
    @Id
    val symbol: String,
    val minPrice: Double,
    val maxPrice: Double,
    val minOrderQty: Double,
    val maxOrderQty: Double,
    val maxLeverage: Double,
    val leverageStep: Double,
    val qtyStep: Double,
) : Serializable
