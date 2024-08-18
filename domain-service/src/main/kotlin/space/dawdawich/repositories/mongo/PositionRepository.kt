package space.dawdawich.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.mongo.entity.PositionDocument

interface PositionRepository : MongoRepository<PositionDocument, String> {
    fun findBySymbolOrderByCloseTimeDesc(symbol: String): List<PositionDocument>
}
