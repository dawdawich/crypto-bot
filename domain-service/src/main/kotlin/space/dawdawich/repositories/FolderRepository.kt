package space.dawdawich.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.entity.FolderDocument

interface FolderRepository : MongoRepository<FolderDocument, String> {

    fun findAllByAccountId(accountId: String): List<FolderDocument>

    fun existsByAccountIdAndName(accountId: String, name: String): Boolean

    fun findByAccountIdAndName(accountId: String, name: String): FolderDocument?

    fun deleteByAccountIdAndName(accountId: String, name: String)
}
