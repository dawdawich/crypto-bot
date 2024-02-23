package space.dawdawich.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import space.dawdawich.controller.model.*
import space.dawdawich.controller.model.folder.*
import space.dawdawich.exception.model.AnalyzerNotFoundException
import space.dawdawich.exception.model.RecordAlreadyExistsException
import space.dawdawich.exception.model.FolderNotFoundException
import space.dawdawich.service.FolderService
import java.util.*

@RestController
@RequestMapping("/folder")
class FolderController(private val folderService: FolderService) {

    @GetMapping
    fun getAllFolders(authentication: Authentication): ResponseEntity<List<GetFolderResponse>> =
            ResponseEntity(folderService.getAllFolders(authentication.name), HttpStatus.OK)

    @PostMapping
    fun createFolder(authentication: Authentication, @RequestBody folderRequest: CreateFolderRequest): ResponseEntity<CreateFolderResponse> =
            try {
                ResponseEntity(folderService.createFolder(authentication.name, folderRequest.name), HttpStatus.CREATED)
            } catch (ex: RecordAlreadyExistsException) {
                ResponseEntity(HttpStatus.CONFLICT)
            }


    @PutMapping("/{id}/analyzers")
    fun analyzersToAdd(authentication: Authentication, @PathVariable id: String, @RequestBody analyzersToAdd: IdListRequest): ResponseEntity<Set<String>> =
            try {
                val analyzers = folderService.addAnalyzersToFolder(authentication.name, id, analyzersToAdd.ids.toSet())
                ResponseEntity(analyzers, HttpStatus.OK)
            } catch (ex: FolderNotFoundException) {
                ResponseEntity(HttpStatus.NOT_FOUND)
            } catch (ex: AnalyzerNotFoundException) {
                ResponseEntity(HttpStatus.BAD_REQUEST)
            }

    @DeleteMapping("/{id}/analyzers")
    fun analyzersToRemove(authentication: Authentication, @PathVariable id: String, @RequestBody analyzersToRemove: IdListRequest): ResponseEntity<Set<String>> =
            try {
                folderService.removeAnalyzersFromFolder(authentication.name, id, analyzersToRemove.ids.toSet())
                ResponseEntity(HttpStatus.OK)
            } catch (ex: FolderNotFoundException) {
                ResponseEntity(HttpStatus.NOT_FOUND)
            } catch (ex: AnalyzerNotFoundException) {
                ResponseEntity(HttpStatus.BAD_REQUEST)
            }

    @GetMapping("/{id}/analyzers")
    fun getAnalyzersInFolder(authentication: Authentication, @PathVariable id: String): ResponseEntity<Set<String>> =
            try {
                ResponseEntity(folderService.getAnalyzersByFolderIdAndAccountId(authentication.name, id), HttpStatus.OK)
            } catch (ex: FolderNotFoundException) {
                ResponseEntity(HttpStatus.NOT_FOUND)
            }

    @GetMapping("/analyzer/{id}")
    fun getAnalyzerFolders(authentication: Authentication, @PathVariable id: String): ResponseEntity<List<GetFolderResponse>> =
        try {
            ResponseEntity(folderService.getFoldersByAnalyzerId(authentication.name, id), HttpStatus.OK)
        } catch (ex: FolderNotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }

    @PatchMapping("/{id}")
    fun updateFolder(
            authentication: Authentication,
            @PathVariable id: String,
            @RequestBody folderRequest: UpdateFolderRequest
    ): ResponseEntity<UpdateFolderResponse> =
            try {
                val updatedFolder = folderService.updateFolder(authentication.name, id, folderRequest.name)
                ResponseEntity(updatedFolder, HttpStatus.OK)
            } catch (ex: FolderNotFoundException) {
                ResponseEntity(HttpStatus.NOT_FOUND)
            } catch (ex: RecordAlreadyExistsException) {
                ResponseEntity(HttpStatus.CONFLICT)
            }

    @DeleteMapping("/{id}")
    fun deleteFolder(authentication: Authentication, @PathVariable id: String): ResponseEntity<Unit> =
            try {
                folderService.deleteFolder(authentication.name, id)
                ResponseEntity(HttpStatus.OK)
            } catch (ex: FolderNotFoundException) {
                ResponseEntity(HttpStatus.NOT_FOUND)
            }

}
