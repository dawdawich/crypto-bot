package space.dawdawich.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.repositories.entity.constants.ManagerStatus

interface TradeManagerRepository : MongoRepository<TradeManagerDocument, String> {

    @Query("{_id: ?0}")
    @Update("{\$set: {status: ?1, stopDescription: ?2, errorDescription: ?3, updateTime: ?4}}")
    fun updateTradeManagerStatus(id: String, status: ManagerStatus, stopDescription: String? = null, errorDescription: String? = null, updateTime: Long = System.currentTimeMillis())

    fun findAllByAccountId(accountId: String): List<TradeManagerDocument>

    fun findAllByStatus(active: ManagerStatus = ManagerStatus.ACTIVE): List<TradeManagerDocument>

    fun findByIdAndAccountId(id: String, accountId: String): TradeManagerDocument?

    fun deleteByIdAndAccountId(id: String, accountId: String): Int
}
