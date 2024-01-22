package space.dawdawich.service

import org.springframework.stereotype.Service
import space.dawdawich.controller.model.folder.CreateFolderResponse
import space.dawdawich.controller.model.folder.GetFolderResponse
import space.dawdawich.controller.model.folder.UpdateFolderResponse
import space.dawdawich.exception.model.FolderNotFoundException
import space.dawdawich.repositories.FolderRepository
import space.dawdawich.repositories.entity.FolderDocument
import space.dawdawich.service.validation.AnalyzerValidationService
import space.dawdawich.service.validation.FolderValidationService

@Service
class FolderService(
        private val folderRepository: FolderRepository,
        private val analyzerValidationService: AnalyzerValidationService,
        private val folderValidationService: FolderValidationService,
) {

    fun getAllFolders(accountId: String): List<GetFolderResponse> =
            folderRepository
                    .findAllByAccountId(accountId)
                    .map { GetFolderResponse(it.id, it.name, it.analyzers) }
                    .toList()

    fun createFolder(accountId: String, name: String): CreateFolderResponse =
            folderValidationService.validateFolderNotExistByName(name, accountId) {
                val folderDocument = folderRepository.insert(FolderDocument(accountId = accountId, name = name))
                CreateFolderResponse(folderDocument.id, folderDocument.name)
            }

    fun updateFolder(accountId: String, folderId: String, newFolderName: String): UpdateFolderResponse =
            folderValidationService.validateFolderNotExistByName(newFolderName, accountId) {
                val folder = getFolderByIdAndAccountId(folderId, accountId)
                folder.name = newFolderName

                val updatedFolder = folderRepository.save(folder)
                UpdateFolderResponse(updatedFolder.id, updatedFolder.name)
            }

    fun deleteFolder(accountId: String, id: String) {
        folderValidationService.validateFolderExistById(id, accountId) {
            folderRepository.deleteByIdAndAccountId(id, accountId)
        }
    }

    fun addAnalyzersToFolder(accountId: String, folderId: String, analyzersToAdd: Set<String>): MutableSet<String> =
            analyzerValidationService.validateAnalyzersExistByIdsAndAccountId(analyzersToAdd, accountId) {
                val folder = getFolderByIdAndAccountId(folderId, accountId)
                folder.analyzers.addAll(analyzersToAdd)
                folderRepository.save(folder).analyzers
            }

    fun removeAnalyzersFromFolder(accountId: String, folderId: String, analyzersToRemove: Set<String>): MutableSet<String> =
            analyzerValidationService.validateAnalyzersExistByIdsAndAccountId(analyzersToRemove, accountId) {
                val folder = getFolderByIdAndAccountId(folderId, accountId)
                folder.analyzers.removeAll(analyzersToRemove)
                folderRepository.save(folder).analyzers
            }

    fun getFolderByIdAndAccountId(folderId: String, accountId: String): FolderDocument =
            folderRepository.findByIdAndAccountId(folderId, accountId)
                    ?: throw FolderNotFoundException("Folder '$folderId' is not found")
}
