package space.dawdawich.service.validation

import org.springframework.stereotype.Service
import space.dawdawich.exception.model.RecordAlreadyExistsException
import space.dawdawich.exception.model.FolderNotFoundException
import space.dawdawich.repositories.mongo.FolderRepository
import kotlin.jvm.Throws

@Service
class FolderValidationService(
        private val folderRepository: FolderRepository,
) {

    @Throws(FolderNotFoundException::class)
    fun validateFolderExistByIdAndAccountId(folderId: String, accountId: String) {
        if (!folderRepository.existsByIdAndAccountId(folderId, accountId)) {
            throw FolderNotFoundException("Folder with id: '$folderId' is not found")
        }
    }

    @Throws(RecordAlreadyExistsException::class)
    fun validateFolderNotExistByNameAndAccountId(name: String, accountId: String) {
        if (folderRepository.existsByNameAndAccountId(name, accountId)) {
            throw RecordAlreadyExistsException("Folder with name: '$name' is already exist")
        }
    }
}
