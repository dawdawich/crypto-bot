package space.dawdawich.service.validation

import org.springframework.stereotype.Service
import space.dawdawich.exception.model.RecordAlreadyExistsException
import space.dawdawich.exception.model.FolderNotFoundException
import space.dawdawich.repositories.mongo.FolderRepository
import kotlin.jvm.Throws

/**
 * The `FolderValidationService` class provides methods for validating folders objects.
 */
@Service
class FolderValidationService(private val folderRepository: FolderRepository) {

    /**
     * Validates if a folder with the given folder ID and account ID exists.
     *
     * @param folderId The ID of the folder to validate.
     * @param accountId The account ID associated with the folder.
     * @throws FolderNotFoundException if the folder is not found.
     */
    @Throws(FolderNotFoundException::class)
    fun validateFolderExistByIdAndAccountId(folderId: String, accountId: String) {
        if (!folderRepository.existsByIdAndAccountId(folderId, accountId)) {
            throw FolderNotFoundException("Folder with id: '$folderId' is not found")
        }
    }

    /**
     * Validates if a folder with the given name and account ID does not exist.
     *
     * @param name The name of the folder to validate.
     * @param accountId The account ID associated with the folder.
     * @throws RecordAlreadyExistsException if the folder already exists.
     */
    @Throws(RecordAlreadyExistsException::class)
    fun validateFolderNotExistByNameAndAccountId(name: String, accountId: String) {
        if (folderRepository.existsByNameAndAccountId(name, accountId)) {
            throw RecordAlreadyExistsException("Folder with name: '$name' is already exist")
        }
    }
}
