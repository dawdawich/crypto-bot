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
            val createdFolder = folderService.createFolder(authentication.name, request.name)
            ResponseEntity(FolderModel(createdFolder.name), HttpStatus.CREATED)
        } catch (ex: EntityAlreadyExistsException) {
            ResponseEntity(HttpStatus.CONFLICT)
        }
    }

    @GetMapping("/{name}/analyzers")
    fun getAnalyzersInFolder(authentication: Authentication, @PathVariable name: String): ResponseEntity<List<String>> {
        return try {
            val analyzers = folderService.getAnalyzersInFolder(authentication.name, name);
            ResponseEntity(analyzers, HttpStatus.OK)
        } catch (ex: NoSuchElementException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @PatchMapping("/{name}")
    fun updateFolder(
            authentication: Authentication,
            @PathVariable name: String,
            @RequestBody request: FolderModel
    ): ResponseEntity<Any> {
        return try {
            val updatedFolder = folderService.updateFolder(authentication.name, name, request.name)
            ResponseEntity(FolderModel(updatedFolder.name), HttpStatus.OK)
        } catch (ex: NoSuchElementException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } catch (ex: EntityAlreadyExistsException) {
            ResponseEntity(HttpStatus.CONFLICT)
        }
    }

    @DeleteMapping("/{name}")
    fun deleteFolder(authentication: Authentication, @PathVariable name: String, ): ResponseEntity<Any> {
        return try {
            folderService.deleteFolder(authentication.name, name)
            ResponseEntity(HttpStatus.OK)
        } catch (ex: NoSuchElementException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
}
