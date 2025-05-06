package space.dawdawich.model

import space.dawdawich.repositories.mongo.entity.SymbolDocument
import java.util.UUID

data class BackTestConfiguration(
    val symbol: SymbolDocument,
    val startCapital: Double,
    val multiplier: Int,
    val diapason: Int,
    val gridSize: Int,
    val takeProfit: Int,
    val stopLoss: Int,
    val id: String = UUID.randomUUID().toString()
)
