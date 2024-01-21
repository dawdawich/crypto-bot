package space.dawdawich.service

import org.springframework.stereotype.Service
import space.dawdawich.controller.model.FolderModel
import space.dawdawich.exception.model.AnalyzerNotFoundException
import space.dawdawich.exception.model.EntityAlreadyExistsException
import space.dawdawich.exception.model.FolderNotFoundException
import space.dawdawich.repositories.FolderRepository
import space.dawdawich.repositories.GridTableAnalyzerRepository
import space.dawdawich.repositories.entity.FolderDocument
import java.util.*

@Service
class FolderService(private val folderRepository: FolderRepository,
                    private val analyzerRepository: GridTableAnalyzerRepository
) {

    fun getAllFolders(accountId: String): List<FolderModel> =
            folderRepository
                    .findAllByAccountId(accountId)
                    .map { FolderModel(it.id, it.name, it.analyzers) }
                    .toList()

    fun createFolder(accountId: String, name: String): FolderDocument {
        if (folderRepository.existsByAccountIdAndName(accountId, name)) {
            throw EntityAlreadyExistsException("Folder '$name' is already exist")
        }
        return folderRepository.insert(FolderDocument(UUID.randomUUID().toString(), accountId, name, emptySet()))
    }

    fun updateFolder(accountId: String, folderId: String, newFolderName: String): FolderDocument {
        if (folderRepository.existsByAccountIdAndName(accountId, newFolderName)) {
            throw EntityAlreadyExistsException("Folder '$newFolderName' is already exist")
        }
        val folder = getFolderByAccountIdAndId(accountId, folderId)
        folder.name = newFolderName
        return folderRepository.save(folder)
    }

    fun deleteFolder(accountId: String, id: String) {
        if (!folderRepository.existsByAccountIdAndId(accountId, id)) {
            throw FolderNotFoundException("Folder '$id' is not found")
        }
        folderRepository.deleteByAccountIdAndId(accountId, id)
    }

    fun addAnalyzersToFolder(accountId: String, id: String, analyzersToAdd: Set<String>): FolderDocument {
        val folder = getFolderByAccountIdAndId(accountId, id)

        validateAnalyzersInAccount(analyzersToAdd, accountId);

        val updatedAnalyzers = folder.analyzers?.toMutableSet()?.apply {
            addAll(analyzersToAdd)
        }

        val updatedFolder = folder.copy(analyzers = updatedAnalyzers)
        return folderRepository.save(updatedFolder)
    }

    fun removeAnalyzersFromFolder(accountId: String, id: String, analyzersToRemove: Set<String>): FolderDocument {
        val folder = getFolderByAccountIdAndId(accountId, id)

        validateAnalyzersInAccount(analyzersToRemove, accountId);

        val updatedAnalyzers = folder.analyzers?.toMutableSet()?.apply {
            removeAll(analyzersToRemove)
        }

        val updatedFolder = folder.copy(analyzers = updatedAnalyzers)
        return folderRepository.save(updatedFolder)
    }

    fun getFolderByAccountIdAndId(accountId: String, id: String): FolderDocument =
            folderRepository.findByAccountIdAndId(accountId, id)
                    ?: throw FolderNotFoundException("Folder '$id' is not found")

    private fun validateAnalyzersInAccount(analyzers: Set<String>, accountId: String) {
        if (analyzers.all { !analyzerRepository.existsByIdAndAccountId(it, accountId) }) {
            throw AnalyzerNotFoundException("This analyzers are not found: $analyzers")
        }
    }
}
