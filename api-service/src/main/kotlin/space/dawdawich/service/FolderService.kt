package space.dawdawich.service

import org.springframework.stereotype.Service
import space.dawdawich.controller.model.FolderModel
import space.dawdawich.exception.model.FolderNotFoundException
import space.dawdawich.repositories.FolderRepository
import space.dawdawich.repositories.entity.FolderDocument
import space.dawdawich.service.validation.AnalyzerValidationService
import space.dawdawich.service.validation.FolderValidationService
import java.util.*

@Service
class FolderService(
        private val folderRepository: FolderRepository,
        private val analyzerValidationService: AnalyzerValidationService,
        private val folderValidationService: FolderValidationService,
) {

    fun getAllFolders(accountId: String): List<FolderModel> =
            folderRepository
                    .findAllByAccountId(accountId)
                    .map { FolderModel(it.id, it.name, it.analyzers) }
                    .toList()

    fun createFolder(accountId: String, name: String): FolderDocument {
        folderValidationService.validateFolderNotExistByName(name, accountId)
        return folderRepository.insert(FolderDocument(UUID.randomUUID().toString(), accountId, name, emptySet()))
    }

    fun updateFolder(accountId: String, folderId: String, newFolderName: String): FolderDocument {
        folderValidationService.validateFolderNotExistByName(newFolderName, accountId)
        val folder = getFolderByIdAndAccountId(folderId, accountId)
        folder.name = newFolderName
        return folderRepository.save(folder)
    }

    fun deleteFolder(accountId: String, id: String) {
        folderValidationService.validateFolderExistById(id, accountId)
        folderRepository.deleteByIdAndAccountId(id, accountId)
    }

    fun addAnalyzersToFolder(accountId: String, folderId: String, analyzersToAdd: Set<String>): FolderDocument {
        analyzerValidationService.validateAnalyzersExistByIds(analyzersToAdd, accountId)

        val folder = getFolderByIdAndAccountId(folderId, accountId)

        val updatedAnalyzers = folder.analyzers?.toMutableSet()?.apply {
            addAll(analyzersToAdd)
        }

        val updatedFolder = folder.copy(analyzers = updatedAnalyzers)
        return folderRepository.save(updatedFolder)
    }

    fun removeAnalyzersFromFolder(accountId: String, folderId: String, analyzersToRemove: Set<String>): FolderDocument {
        analyzerValidationService.validateAnalyzersExistByIds(analyzersToRemove, accountId)

        val folder = getFolderByIdAndAccountId(folderId, accountId)

        val updatedAnalyzers = folder.analyzers?.toMutableSet()?.apply {
            removeAll(analyzersToRemove)
        }

        val updatedFolder = folder.copy(analyzers = updatedAnalyzers)
        return folderRepository.save(updatedFolder)
    }

    fun getFolderByIdAndAccountId(folderId: String, accountId: String): FolderDocument =
            folderRepository.findByIdAndAccountId(folderId, accountId)
                    ?: throw FolderNotFoundException("Folder '$folderId' is not found")
}
