package space.dawdawich.repositories.mongo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.io.Serializable
import java.util.*

@Document("price_tick") // 14 days
class PriceTickModel(
    @Indexed
    val pair: Int,
    val price: Double,
    @Indexed
    val time: Long,
    @Id
    val id: Int = Objects.hash(pair, time)
) : Serializable
