package space.dawdawich.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import space.dawdawich.controller.model.*
import space.dawdawich.exception.model.AnalyzerNotFoundException
import space.dawdawich.exception.model.EntityAlreadyExistsException
import space.dawdawich.exception.model.FolderNotFoundException
import space.dawdawich.service.FolderService
import java.util.*

@RestController
@RequestMapping("/folder")
class FolderController(private val folderService: FolderService) {

    @GetMapping
    fun getAllFolders(authentication: Authentication): ResponseEntity<List<FolderModel>> =
            ResponseEntity(folderService.getAllFolders(authentication.name), HttpStatus.OK)

    @PostMapping
    fun createFolder(authentication: Authentication, @RequestBody folderRequest: FolderModel): ResponseEntity<Any> {
        return try {
            val createdFolder = folderService.createFolder(authentication.name, folderRequest.name)
            ResponseEntity(FolderModel(createdFolder.id, createdFolder.name), HttpStatus.CREATED)
        } catch (ex: EntityAlreadyExistsException) {
            ResponseEntity(HttpStatus.CONFLICT)
        }
    }

    @PutMapping("/{id}/analyzers")
    fun addAnalyzersToFolder(authentication: Authentication, @PathVariable id: String, @RequestBody analyzersToAdd: Set<String>): ResponseEntity<Any> {
        return try {
            val updatedFolder = folderService.addAnalyzersToFolder(authentication.name, id, analyzersToAdd)
            ResponseEntity(updatedFolder.analyzers, HttpStatus.OK)
        } catch (ex: FolderNotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } catch (ex: AnalyzerNotFoundException) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    @DeleteMapping("/{id}/analyzers")
    fun removeAnalyzersFromFolder(authentication: Authentication, @PathVariable id: String, @RequestBody analyzersToAdd: Set<String>): ResponseEntity<Any> {
        return try {
            val updatedFolder = folderService.removeAnalyzersFromFolder(authentication.name, id, analyzersToAdd)
            ResponseEntity(updatedFolder.analyzers, HttpStatus.OK)
        } catch (ex: FolderNotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } catch (ex: AnalyzerNotFoundException) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    @GetMapping("/{id}/analyzers")
    fun getAnalyzersInFolder(authentication: Authentication, @PathVariable id: String): ResponseEntity<Set<String>> {
        return try {
            val analyzers = folderService.getFolderByAccountIdAndId(authentication.name, id).analyzers ?: emptySet()
            ResponseEntity(analyzers, HttpStatus.OK)
        } catch (ex: FolderNotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @PatchMapping("/{id}")
    fun updateFolder(
            authentication: Authentication,
            @PathVariable id: String,
            @RequestBody folderRequest: FolderModel
    ): ResponseEntity<Any> {
        return try {
            val updatedFolder = folderService.updateFolder(authentication.name, id, folderRequest.name)
            ResponseEntity(FolderModel(updatedFolder.id, updatedFolder.name), HttpStatus.OK)
        } catch (ex: FolderNotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } catch (ex: EntityAlreadyExistsException) {
            ResponseEntity(HttpStatus.CONFLICT)
        }
    }

    @DeleteMapping("/{id}")
    fun deleteFolder(authentication: Authentication, @PathVariable id: String): ResponseEntity<Any> {
        return try {
            folderService.deleteFolder(authentication.name, id)
            ResponseEntity(HttpStatus.OK)
        } catch (ex: FolderNotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
}
