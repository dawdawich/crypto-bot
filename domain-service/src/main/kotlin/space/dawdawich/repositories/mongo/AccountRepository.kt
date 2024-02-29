package space.dawdawich.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import space.dawdawich.repositories.mongo.entity.AccountDocument

interface AccountRepository : MongoRepository<AccountDocument, String> {
    @Query("{ walletId: ?0 }")
    @Update("{ \$set: { lastCheckedBlock: ?1 } }")
    fun updateLastCheckedBlock(accountId: String, lastCheckedBlock: Long)
}
