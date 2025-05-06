package space.dawdawich.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.mongo.entity.AccountTransactionDocument

interface AccountTransactionRepository : MongoRepository<AccountTransactionDocument, String> {
    fun findByAccountId(accountId: String): List<AccountTransactionDocument>

}
