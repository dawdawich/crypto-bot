package space.dawdawich.service

import org.springframework.stereotype.Service
import space.dawdawich.controller.model.folder.CreateFolderResponse
import space.dawdawich.controller.model.folder.GetFolderResponse
import space.dawdawich.controller.model.folder.UpdateFolderResponse
import space.dawdawich.exception.model.FolderNotFoundException
import space.dawdawich.repositories.FolderAnalyzerRepository
import space.dawdawich.repositories.FolderRepository
import space.dawdawich.repositories.entity.FolderAnalyzerDocument
import space.dawdawich.repositories.entity.FolderDocument
import space.dawdawich.service.validation.AnalyzerValidationService
import space.dawdawich.service.validation.FolderValidationService

@Service
class FolderService(
        private val folderRepository: FolderRepository,
        private val folderAnalyzerRepository: FolderAnalyzerRepository,
        private val analyzerValidationService: AnalyzerValidationService,
        private val folderValidationService: FolderValidationService
) {

    fun getAllFolders(accountId: String): List<GetFolderResponse> =
            folderRepository
                    .findAllByAccountId(accountId)
                    .map { GetFolderResponse(it.id, it.name, getAnalyzersByFolderId(it.id)) }
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

    fun deleteFolder(accountId: String, id: String) =
            folderValidationService.validateFolderExistById(id, accountId) {
                folderRepository.deleteByIdAndAccountId(id, accountId)
            }

    fun getAnalyzersByFolderIdAndAccountId(accountId: String, folderId: String): MutableSet<String> =
            folderValidationService.validateFolderExistById(folderId, accountId) {
                getAnalyzersByFolderId(folderId)
            }

    fun addAnalyzersToFolder(accountId: String, folderId: String, analyzerIds: MutableSet<String>): MutableSet<String> =
            analyzerValidationService.validateAnalyzersExistByIdsAndAccountId(analyzerIds, accountId) {
                folderValidationService.validateFolderExistById(folderId, accountId) {
                    val existingAnalyzers = getAnalyzersByFolderId(folderId)
                    analyzerIds.filterNot { existingAnalyzers.contains(it) }
                            .map { FolderAnalyzerDocument(folderId = folderId, analyzerId = it) }
                            .let { folderAnalyzerRepository.saveAll(it) }

                    (existingAnalyzers + analyzerIds).toMutableSet()
                }
            }

    fun removeAnalyzersFromFolder(accountId: String, folderId: String, analyzerIds: Set<String>): Set<String> =
            analyzerValidationService.validateAnalyzersExistByIdsAndAccountId(analyzerIds, accountId) {
                folderValidationService.validateFolderExistById(folderId, accountId) {
                    val existingAnalyzers = getAnalyzersByFolderId(folderId)
                    analyzerIds.filter { existingAnalyzers.contains(it) }.let { folderAnalyzerRepository.deleteByAnalyzerIdIn(analyzerIds) }
                    (existingAnalyzers - analyzerIds).toMutableSet()
                }
            }

    private fun getAnalyzersByFolderId(folderId: String): MutableSet<String> =
            folderAnalyzerRepository.findAllByFolderId(folderId).map { it.analyzerId }.toMutableSet()

    private fun getFolderByIdAndAccountId(folderId: String, accountId: String): FolderDocument =
            folderRepository.findByIdAndAccountId(folderId, accountId)
                    ?: throw FolderNotFoundException("Folder '$folderId' is not found")
}
