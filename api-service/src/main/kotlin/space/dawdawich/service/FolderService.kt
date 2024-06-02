package space.dawdawich.service

import org.springframework.stereotype.Service
import space.dawdawich.controller.model.folder.CreateFolderResponse
import space.dawdawich.controller.model.folder.GetFolderResponse
import space.dawdawich.controller.model.folder.UpdateFolderResponse
import space.dawdawich.exception.model.AnalyzerNotFoundException
import space.dawdawich.exception.model.FolderNotFoundException
import space.dawdawich.exception.model.RecordAlreadyExistsException
import space.dawdawich.repositories.mongo.AnalyzerRepository
import space.dawdawich.repositories.mongo.FolderAnalyzerRepository
import space.dawdawich.repositories.mongo.FolderRepository
import space.dawdawich.repositories.mongo.entity.FolderAnalyzerDocument
import space.dawdawich.repositories.mongo.entity.FolderDocument
import space.dawdawich.service.validation.AnalyzerValidationService
import space.dawdawich.service.validation.FolderValidationService

/**
 * The `FolderService` class provides methods for performing operations related to folders.
 *
 * @param folderRepository The repository for Folder documents.
 * @param folderAnalyzerRepository The repository for FolderAnalyzer documents.
 * @param analyzerValidationService The service for analyzing the existence of analyzers.
 * @param folderValidationService The service for validating the folder objects.
 * @param analyzerRepository The repository for Analyzer documents.
 */
@Service
class FolderService(
    private val folderRepository: FolderRepository,
    private val folderAnalyzerRepository: FolderAnalyzerRepository,
    private val analyzerValidationService: AnalyzerValidationService,
    private val folderValidationService: FolderValidationService,
    private val analyzerRepository: AnalyzerRepository,
) {

    /**
     * Retrieves all folders for the given account ID.
     *
     * @param accountId The ID of the account.
     * @return The list of GetFolderResponse objects representing the folders.
     */
    fun getAllFolders(accountId: String): List<GetFolderResponse> =
        folderRepository
            .findAllByAccountId(accountId)
            .map { GetFolderResponse(it.id, it.name) }
            .toList()

    /**
     * Creates a new folder with the specified account ID and name.
     *
     * @param accountId The ID of the account associated with the folder.
     * @param name The name of the folder to be created.
     * @return The [CreateFolderResponse] object representing the created folder.
     * @throws [RecordAlreadyExistsException] if folder with the same name already exist
     */
    fun createFolder(accountId: String, name: String): CreateFolderResponse =
        folderValidationService
            .validateFolderNotExistByNameAndAccountId(name, accountId)
            .let { folderRepository.insert(FolderDocument(accountId = accountId, name = name)) }
            .let { CreateFolderResponse(it.id, it.name) }

    /**
     * Updates the name of a folder with the specified account ID and folder ID.
     *
     * @param accountId The ID of the account associated with the folder.
     * @param folderId The ID of the folder to update.
     * @param newFolderName The new name for the folder.
     * @return The updated folder as an instance of [UpdateFolderResponse].
     * @throws [RecordAlreadyExistsException] if folder with the same name already exist
     */
    fun updateFolder(accountId: String, folderId: String, newFolderName: String): UpdateFolderResponse =
        folderValidationService
            .validateFolderNotExistByNameAndAccountId(newFolderName, accountId)
            .let { getFolderByIdAndAccountId(folderId, accountId).copy(name = newFolderName) }
            .let { folderRepository.save(it) }
            .let { UpdateFolderResponse(it.id, it.name) }

    /**
     * Deletes a folder with the specified ID, belonging to the specified account.
     *
     * @param accountId The ID of the account to which the folder belongs.
     * @param id The ID of the folder to be deleted.
     * @throws [FolderNotFoundException] if folder not found for this account.
     */
    fun deleteFolder(accountId: String, id: String) =
        folderValidationService
            .validateFolderExistByIdAndAccountId(id, accountId)
            .let { folderRepository.deleteByIdAndAccountId(id, accountId) }
            .let { folderAnalyzerRepository.deleteByFolderId(id) }

    /**
     * Retrieves the set of analyzers by the given folder ID and account ID.
     *
     * @param accountId The ID of the account.
     * @param folderId The ID of the folder.
     * @return A set of analyzer IDs.
     * @throws [FolderNotFoundException] if folder not found for this account.
     */
    fun getAnalyzersByFolderIdAndAccountId(accountId: String, folderId: String): Set<String> =
        folderValidationService
            .validateFolderExistByIdAndAccountId(folderId, accountId)
            .let { getAnalyzersByFolderId(folderId) }

    /**
     * Retrieves a list of folders associated with a given analyzer ID.
     *
     * @param accountId The ID of the account.
     * @param analyzerId The ID of the analyzer.
     * @return A list of GetFolderResponse objects representing the folders.
     * @throws [AnalyzerNotFoundException] if analyzer not found for this account.
     */
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

    /**
     * Adds analyzers to a folder.
     *
     * @param accountId The ID of the account.
     * @param folderId The ID of the folder.
     * @param analyzerIds The set of analyzer IDs to add.
     * @param all Determines whether to add all analyzers or not. Default is false.
     * @return The updated set of analyzer IDs in the folder.
     * @throws [AnalyzerNotFoundException] if analyzer not found for this account.
     */
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

    /**
     * Removes analyzers from a folder.
     *
     * @param accountId The ID of the account to which the folder belongs.
     * @param folderId The ID of the folder from which analyzers should be removed.
     * @param analyzersToRemove A set of analyzer IDs to be removed from the folder.
     */
    fun removeAnalyzersFromFolder(accountId: String, folderId: String, analyzersToRemove: Set<String>) {
        folderAnalyzerRepository.deleteByFolderIdAndAnalyzerIdIn(folderId, analyzersToRemove)
    }

    /**
     * Removes the specified analyzers from a folder.
     *
     * @param analyzersToRemove A set of analyzer IDs to be removed from the folder.
     */
    fun removeAnalyzers(analyzersToRemove: Set<String>) {
        folderAnalyzerRepository.deleteByAnalyzerIdIn(analyzersToRemove)
    }

    /**
     * Retrieves the set of analyzer IDs for a folder specified by its ID.
     *
     * @param folderId The ID of the folder.
     * @return A mutable set of analyzer IDs.
     */
    private fun getAnalyzersByFolderId(folderId: String): MutableSet<String> =
        folderAnalyzerRepository
            .findAllByFolderId(folderId).map { it.analyzerId }
            .toMutableSet()

    /**
     * Retrieves the folder document by its ID and account ID.
     *
     * @param folderId The ID of the folder.
     * @param accountId The ID of the account.
     * @return The FolderDocument object representing the folder.
     * @throws FolderNotFoundException If the folder with the specified ID is not found.
     */
    private fun getFolderByIdAndAccountId(folderId: String, accountId: String): FolderDocument =
        folderRepository.findByIdAndAccountId(folderId, accountId)
            ?: throw FolderNotFoundException("Folder '$folderId' is not found")
}
