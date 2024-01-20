package space.dawdawich.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.entity.FolderDocument

interface FolderRepository : MongoRepository<FolderDocument, String> {

    fun findAllByAccountId(accountId: String): List<FolderDocument>

    fun existsByAccountIdAndFolderId(accountId: String, folderId: String): Boolean

    fun findByAccountIdAndFolderId(accountId: String, folderId: String): FolderDocument?

    fun deleteByAccountIdAndFolderId(accountId: String, folderId: String)
}
