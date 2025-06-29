package space.dawdawich.repositories.mongo.entity

import org.springframework.data.mongodb.core.mapping.Document
import space.dawdawich.model.Market
import java.io.Serializable

@Document(collection = "symbol")
data class SymbolDocument(
    val symbol: String,
    val minPrice: Double,
    val maxPrice: Double,
    val minOrderQty: Double,
    val maxOrderQty: Double,
    val maxLeverage: Double,
    val leverageStep: Double,
    val qtyStep: Double,
    val market: Market,
    val launchTime: Long = 0
) : Serializable
