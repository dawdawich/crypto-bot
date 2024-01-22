package space.dawdawich.service.validation

import org.springframework.stereotype.Service
import space.dawdawich.exception.model.EntityAlreadyExistsException
import space.dawdawich.exception.model.FolderNotFoundException
import space.dawdawich.repositories.FolderRepository
import kotlin.jvm.Throws

@Service
class FolderValidationService(
        private val folderRepository: FolderRepository,
) {

    @Throws(FolderNotFoundException::class)
    fun <T> validateFolderExistById(folderId: String, accountId: String, action: () -> T): T {
        if (!folderRepository.existsByIdAndAccountId(folderId, accountId)) {
            throw FolderNotFoundException("Folder with id: '$folderId' is not found")
        }
        return action()
    }

    @Throws(EntityAlreadyExistsException::class)
    fun <T> validateFolderNotExistByName(name: String, accountId: String, action: () -> T): T {
        if (folderRepository.existsByNameAndAccountId(name, accountId)) {
            throw EntityAlreadyExistsException("Folder with name: '$name' is already exist")
        }
        return action()
    }
}
