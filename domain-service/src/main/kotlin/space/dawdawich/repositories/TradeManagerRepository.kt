package space.dawdawich.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import space.dawdawich.repositories.entity.TradeManagerDocument

interface TradeManagerRepository : MongoRepository<TradeManagerDocument, String> {
    @Query("{_id: ?0, accountId:  ?1}")
    @Update("{\$set: {isActive: ?2}}")
    fun updateTradeManagerStatus(id: String, accountId: String, isActive: Boolean)

    fun findAllByAccountId(accountId: String): List<TradeManagerDocument>

    fun findByIdAndAccountId(id: String, accountId: String): TradeManagerDocument?
}
