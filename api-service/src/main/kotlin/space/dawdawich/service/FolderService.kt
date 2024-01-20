package space.dawdawich.service

import org.springframework.stereotype.Service
import space.dawdawich.exception.model.EntityAlreadyExistsException
import space.dawdawich.repositories.FolderRepository
import space.dawdawich.repositories.entity.FolderDocument
import java.util.*
import kotlin.NoSuchElementException

@Service
class FolderService(private val folderRepository: FolderRepository) {

    fun getAllFolders(accountId: String): List<String> =
            folderRepository.findAllByAccountId(accountId).map { it.name }.toList()

    fun createFolder(accountId: String, name: String): FolderDocument {
        if (isFolderExist(accountId, name)) {
            throw EntityAlreadyExistsException("Folder '$name' is already exist")
        }
        return folderRepository.insert(FolderDocument(UUID.randomUUID().toString(), accountId, name, emptyList()))
    }

    fun updateFolder(accountId: String, oldName: String, newName: String): FolderDocument {
        if (isFolderExist(accountId, newName)) {
            throw EntityAlreadyExistsException("Folder '$newName' is already exist")
        }
        val folder = findFolderByAccountIdAndName(accountId, oldName)
        folder.name = newName
        return folderRepository.save(folder)
    }

    fun deleteFolder(accountId: String, name: String) {
        if (!isFolderExist(accountId, name)) {
            throw NoSuchElementException("Folder '$name' is not found")
        }
        folderRepository.deleteByAccountIdAndName(accountId, name)
    }

    fun getAnalyzersInFolder(accountId: String, name: String): List<String> =
            findFolderByAccountIdAndName(accountId, name).analyzers ?: emptyList()

    private fun findFolderByAccountIdAndName(accountId: String, name: String): FolderDocument =
            folderRepository.findByAccountIdAndName(accountId, name)
                    ?: throw NoSuchElementException("Folder '$name' is not found")

    private fun isFolderExist(accountId: String, name: String): Boolean =
            folderRepository.existsByAccountIdAndName(accountId, name)
}
