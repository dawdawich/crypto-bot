package space.dawdawich.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.entity.ApiAccessTokenDocument

interface ApiAccessTokenRepository : MongoRepository<ApiAccessTokenDocument, String> {
    fun findAllByAccountId(accountId: String): List<ApiAccessTokenDocument>
    fun deleteByIdAndAccountId(id: String, accountId: String): Int
}