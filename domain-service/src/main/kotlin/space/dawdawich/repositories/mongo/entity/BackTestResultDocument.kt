package space.dawdawich.repositories.mongo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("back_test_result")
class BackTestResultDocument(
    @Id
    val id: String,
    val requestId: String,
    val symbol: String,
    val startCapital: Double,
    val multiplier: Double,
    val diapason: Int,
    val gridSize: Int,
    val takeProfit: Int,
    val stopLoss: Int,
    val startTime: Long,
    val endTime: Long,
    val finalCapital: Double
)
