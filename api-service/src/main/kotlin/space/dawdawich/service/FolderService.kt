package space.dawdawich.service

import org.springframework.stereotype.Service
import space.dawdawich.controller.model.folder.CreateFolderResponse
import space.dawdawich.controller.model.folder.GetFolderResponse
import space.dawdawich.controller.model.folder.UpdateFolderResponse
import space.dawdawich.exception.model.FolderNotFoundException
import space.dawdawich.repositories.mongo.AnalyzerRepository
import space.dawdawich.repositories.mongo.FolderAnalyzerRepository
import space.dawdawich.repositories.mongo.FolderRepository
import space.dawdawich.repositories.mongo.entity.FolderAnalyzerDocument
import space.dawdawich.repositories.mongo.entity.FolderDocument
import space.dawdawich.service.validation.AnalyzerValidationService
import space.dawdawich.service.validation.FolderValidationService

@Service
class FolderService(
    private val folderRepository: FolderRepository,
    private val folderAnalyzerRepository: FolderAnalyzerRepository,
    private val analyzerValidationService: AnalyzerValidationService,
    private val folderValidationService: FolderValidationService,
    private val analyzerRepository: AnalyzerRepository,
) {

    fun getAllFolders(accountId: String): List<GetFolderResponse> =
        folderRepository
            .findAllByAccountId(accountId)
            .map { GetFolderResponse(it.id, it.name) }
            .toList()

    fun createFolder(accountId: String, name: String): CreateFolderResponse =
        folderValidationService
            .validateFolderNotExistByNameAndAccountId(name, accountId)
            .let { folderRepository.insert(FolderDocument(accountId = accountId, name = name)) }
            .let { CreateFolderResponse(it.id, it.name) }

    fun updateFolder(accountId: String, folderId: String, newFolderName: String): UpdateFolderResponse =
        folderValidationService
            .validateFolderNotExistByNameAndAccountId(newFolderName, accountId)
            .let { getFolderByIdAndAccountId(folderId, accountId).copy(name = newFolderName) }
            .let { folderRepository.save(it) }
            .let { UpdateFolderResponse(it.id, it.name) }

    fun deleteFolder(accountId: String, id: String) =
        folderValidationService
            .validateFolderExistByIdAndAccountId(id, accountId)
            .let { folderRepository.deleteByIdAndAccountId(id, accountId) }
            .let { folderAnalyzerRepository.deleteByFolderId(id) }

    fun getAnalyzersByFolderIdAndAccountId(accountId: String, folderId: String): Set<String> =
        folderValidationService
            .validateFolderExistByIdAndAccountId(folderId, accountId)
            .let { getAnalyzersByFolderId(folderId) }

    fun getFoldersByAnalyzerId(accountId: String, analyzerId: String): List<GetFolderResponse> =
        analyzerValidationService
            .validateAnalyzerExistByIdAndAccountId(analyzerId, accountId)
            .let {
                folderAnalyzerRepository.findAllByAnalyzerId(analyzerId)
                    .map { analyzerFolder -> analyzerFolder.folderId }
            }
            .let { folderRepository.findAllByAccountIdAndIdIn(accountId, it) }
            .map { GetFolderResponse(it.id, it.name) }
            .toList()

    fun addAnalyzersToFolder(
        accountId: String,
        folderId: String,
        analyzerIds: Set<String>,
        all: Boolean = false,
    ): Set<String> = if (!all) {
        analyzerValidationService
            .validateAnalyzersExistByIdsAndAccountId(analyzerIds, accountId)
            .let { folderValidationService.validateFolderExistByIdAndAccountId(folderId, accountId) }
            .let { getAnalyzersByFolderId(folderId) }
            .let { existingAnalyzers ->
                analyzerIds
                    .filterNot { existingAnalyzers.contains(it) }
                    .map { FolderAnalyzerDocument(folderId = folderId, analyzerId = it) }
                    .let { folderAnalyzerRepository.saveAll(it) }
                    .let { (existingAnalyzers + analyzerIds).toMutableSet() }
            }
    } else {
        getAnalyzersByFolderId(folderId)
            .let { existingAnalyzers ->
                analyzerRepository.findAllByAccountIdAndIdNotIn(accountId, analyzerIds)
                    .map { analyzer -> analyzer.id }
                    .filterNot { id -> existingAnalyzers.contains(id) }
                    .map { FolderAnalyzerDocument(folderId = folderId, analyzerId = it) }
                    .let { folderAnalyzerRepository.saveAll(it) }
                    .let { (existingAnalyzers + analyzerIds).toMutableSet() }
            }
    }

    fun removeAnalyzersFromFolder(accountId: String, folderId: String, analyzersToRemove: Set<String>) {
        folderAnalyzerRepository.deleteByFolderIdAndAnalyzerIdIn(folderId, analyzersToRemove)
    }

    fun removeAnalyzers(analyzersToRemove: Set<String>) {
        folderAnalyzerRepository.deleteByAnalyzerIdIn(analyzersToRemove)
    }

    fun analyzersCount(folderId: String) = folderAnalyzerRepository.countByFolderId(folderId)

    private fun getAnalyzersByFolderId(folderId: String): MutableSet<String> =
        folderAnalyzerRepository
            .findAllByFolderId(folderId).map { it.analyzerId }
            .toMutableSet()

    private fun getFolderByIdAndAccountId(folderId: String, accountId: String): FolderDocument =
        folderRepository.findByIdAndAccountId(folderId, accountId)
            ?: throw FolderNotFoundException("Folder '$folderId' is not found")
}
