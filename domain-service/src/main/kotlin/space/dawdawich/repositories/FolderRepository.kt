package space.dawdawich.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.entity.FolderDocument

interface FolderRepository : MongoRepository<FolderDocument, String> {

    fun findAllByAccountId(accountId: String): List<FolderDocument>
    fun findAllByAccountIdAndIdIn(accountId: String, ids: List<String>): List<FolderDocument>

    fun existsByIdAndAccountId(id: String, accountId: String): Boolean

    fun existsByNameAndAccountId(name: String, accountId: String): Boolean

    fun findByIdAndAccountId(id: String, accountId: String): FolderDocument?

    fun deleteByIdAndAccountId(id: String, accountId: String)

}
