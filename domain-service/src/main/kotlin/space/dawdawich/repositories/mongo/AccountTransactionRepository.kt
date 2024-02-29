package space.dawdawich.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import space.dawdawich.repositories.mongo.entity.AccountTransactionDocument
import kotlin.time.Duration.Companion.days

interface AccountTransactionRepository : MongoRepository<AccountTransactionDocument, String> {
    fun findByAccountId(accountId: String): List<AccountTransactionDocument>

    @Query("{ accountId: ?0, time: { \$gt: ?1 } }")
    fun getActiveTransactionsForTimeRange(accountId: String, time: Long = ((System.currentTimeMillis() - 30.days.inWholeMilliseconds) / 1000)): List<AccountTransactionDocument>
}
