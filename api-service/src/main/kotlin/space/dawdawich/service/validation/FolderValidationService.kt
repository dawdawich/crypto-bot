package space.dawdawich.service.validation

import org.springframework.stereotype.Service
import space.dawdawich.exception.model.EntityAlreadyExistsException
import space.dawdawich.exception.model.FolderNotFoundException
import space.dawdawich.repositories.FolderRepository

@Service
class FolderValidationService(
        private val folderRepository: FolderRepository,
) {
    fun validateFolderExistById(folderId: String, accountId: String) {
        if (!folderRepository.existsByIdAndAccountId(folderId, accountId))
            throw FolderNotFoundException("Folder with id: '$folderId' is not found")
    }

    fun validateFolderNotExistByName(name: String, accountId: String) {
        if (folderRepository.existsByNameAndAccountId(name, accountId))
            throw EntityAlreadyExistsException("Folder with name: '$name' is already exist")
    }
}
