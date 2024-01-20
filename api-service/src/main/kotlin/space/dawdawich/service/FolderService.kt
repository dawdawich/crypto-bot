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
            folderRepository.findAllByAccountId(accountId).map { it.folderId }.toList()

    fun createFolder(accountId: String, folderId: String): FolderDocument {
        if (isFolderExist(accountId, folderId)) {
            throw EntityAlreadyExistsException("Folder '$folderId' is already exist")
        }
        return folderRepository.insert(FolderDocument(UUID.randomUUID().toString(), accountId, folderId, emptyList()))
    }

    fun updateFolder(accountId: String, oldFolderId: String, newFolderId: String): FolderDocument {
        if (isFolderExist(accountId, newFolderId)) {
            throw EntityAlreadyExistsException("Folder '$newFolderId' is already exist")
        }
        val folder = findFolderByAccountIdAndFolderId(accountId, oldFolderId)
        folder.folderId = newFolderId
        return folderRepository.save(folder)
    }

    fun deleteFolder(accountId: String, folderId: String) {
        if (!isFolderExist(accountId, folderId)) {
            throw NoSuchElementException("Folder '$folderId' is not found")
        }
        folderRepository.deleteByAccountIdAndFolderId(accountId, folderId)
    }

    fun getAnalyzersInFolder(accountId: String, folderId: String): List<String> =
            findFolderByAccountIdAndFolderId(accountId, folderId).analyzers ?: emptyList()

    private fun findFolderByAccountIdAndFolderId(accountId: String, folderId: String): FolderDocument =
            folderRepository.findByAccountIdAndFolderId(accountId, folderId)
                    ?: throw NoSuchElementException("Folder '$folderId' is not found")

    private fun isFolderExist(accountId: String, folderId: String): Boolean =
            folderRepository.existsByAccountIdAndFolderId(accountId, folderId)
}
