package space.dawdawich.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import space.dawdawich.controller.model.*
import space.dawdawich.exception.model.EntityAlreadyExistsException
import space.dawdawich.service.FolderService

@RestController
@RequestMapping("/folder")
class FolderController(private val folderService: FolderService) {

    @GetMapping
    fun getAllFolders(authentication: Authentication): ResponseEntity<List<String>> {
        return ResponseEntity(folderService.getAllFolders(authentication.name), HttpStatus.OK)
    }

    @PostMapping
    fun createFolder(authentication: Authentication, @RequestBody request: FolderModel): ResponseEntity<Any> {
        return try {
            val createdFolder = folderService.createFolder(authentication.name, request.folderId)
            ResponseEntity(FolderModel(createdFolder.folderId, createdFolder.analyzers), HttpStatus.CREATED)
        } catch (ex: EntityAlreadyExistsException) {
            ResponseEntity(HttpStatus.CONFLICT)
        }
    }

    @GetMapping("/{folderId}/analyzers")
    fun getAnalyzersInFolder(authentication: Authentication, @PathVariable folderId: String): ResponseEntity<List<String>> {
        return try {
            val analyzers = folderService.getAnalyzersInFolder(authentication.name, folderId);
            ResponseEntity(analyzers, HttpStatus.OK)
        } catch (ex: NoSuchElementException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @PatchMapping("/{folderId}")
    fun updateFolder(
            authentication: Authentication,
            @PathVariable folderId: String,
            @RequestBody request: FolderModel
    ): ResponseEntity<Any> {
        return try {
            val updatedFolder = folderService.updateFolder(authentication.name, folderId, request.folderId)
            ResponseEntity(FolderModel(updatedFolder.folderId, updatedFolder.analyzers), HttpStatus.OK)
        } catch (ex: NoSuchElementException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } catch (ex: EntityAlreadyExistsException) {
            ResponseEntity(HttpStatus.CONFLICT)
        }
    }

    @DeleteMapping("/{folderId}")
    fun deleteFolder(authentication: Authentication, @PathVariable folderId: String, ): ResponseEntity<Any> {
        return try {
            folderService.deleteFolder(authentication.name, folderId)
            ResponseEntity(HttpStatus.OK)
        } catch (ex: NoSuchElementException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
}
