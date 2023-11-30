package space.dawdawich.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import space.dawdawich.repositories.entity.TradeManagerDocument

interface TradeManagerRepository : MongoRepository<TradeManagerDocument, String> {
    @Query("{_id: ?0}")
    @Update("{isActive: ?1}")
    fun updateTradeManagerStatus(id: String, isActive: Boolean)
}
