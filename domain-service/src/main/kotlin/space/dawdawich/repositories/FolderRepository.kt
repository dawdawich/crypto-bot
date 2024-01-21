package space.dawdawich.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.entity.FolderDocument

interface FolderRepository : MongoRepository<FolderDocument, String> {

    fun findAllByAccountId(accountId: String): List<FolderDocument>

    fun existsByAccountIdAndId(accountId: String, id: String): Boolean

    fun existsByAccountIdAndName(accountId: String, name: String): Boolean

    fun findByAccountIdAndId(accountId: String, id: String): FolderDocument?

    fun deleteByAccountIdAndId(accountId: String, id: String)
}
