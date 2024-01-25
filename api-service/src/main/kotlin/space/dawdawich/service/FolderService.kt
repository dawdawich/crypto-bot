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

    fun getAnalyzersByFolderIdAndAccountId(accountId: String, folderId: String): MutableSet<String> =
            folderValidationService
                    .validateFolderExistByIdAndAccountId(folderId, accountId)
                    .let { getAnalyzersByFolderId(folderId) }

    fun addAnalyzersToFolder(accountId: String, folderId: String, analyzersToAdd: MutableSet<String>): MutableSet<String> =
            analyzerValidationService
                    .validateAnalyzersExistByIdsAndAccountId(analyzersToAdd, accountId)
                    .let { folderValidationService.validateFolderExistByIdAndAccountId(folderId, accountId) }
                    .let { getAnalyzersByFolderId(folderId) }
                    .let { existingAnalyzers ->
                        analyzersToAdd
                                .filterNot { existingAnalyzers.contains(it) }
                                .map { FolderAnalyzerDocument(folderId = folderId, analyzerId = it) }
                                .let { folderAnalyzerRepository.saveAll(it) }
                                .let { (existingAnalyzers + analyzersToAdd).toMutableSet() }
                    }

    fun removeAnalyzersFromFolder(accountId: String, folderId: String, analyzersToRemove: Set<String>): Set<String> =
            analyzerValidationService
                    .validateAnalyzersExistByIdsAndAccountId(analyzersToRemove, accountId)
                    .let { folderValidationService.validateFolderExistByIdAndAccountId(folderId, accountId) }
                    .let { getAnalyzersByFolderId(folderId) }
                    .let { existingAnalyzers ->
                        analyzersToRemove
                                .filter { existingAnalyzers.contains(it) }
                                .let { folderAnalyzerRepository.deleteByAnalyzerIdIn(analyzersToRemove) }
                                .let { (existingAnalyzers - analyzersToRemove).toMutableSet() }
                    }

    private fun getAnalyzersByFolderId(folderId: String): MutableSet<String> =
            folderAnalyzerRepository
                    .findAllByFolderId(folderId).map { it.analyzerId }
                    .toMutableSet()

    private fun getFolderByIdAndAccountId(folderId: String, accountId: String): FolderDocument =
            folderRepository.findByIdAndAccountId(folderId, accountId)
                    ?: throw FolderNotFoundException("Folder '$folderId' is not found")
}
