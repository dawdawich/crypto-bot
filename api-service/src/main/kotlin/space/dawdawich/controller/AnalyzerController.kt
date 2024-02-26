package space.dawdawich.controller

import jakarta.annotation.security.RolesAllowed
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import space.dawdawich.controller.model.*
import space.dawdawich.exception.model.AnalyzerNotFoundException
import space.dawdawich.service.AnalyzerService

@RestController
@RequestMapping("/analyzer")
class AnalyzerController(private val analyzerService: AnalyzerService) {

    @GetMapping("/top20")
    fun getTopAnalyzers(): List<GridTableAnalyzerResponse> = analyzerService.getTopAnalyzers()

    @GetMapping
    fun getAnalyzers(
        authentication: Authentication,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "10") size: Int,
        @RequestParam("status", required = false) status: Boolean?,
        @RequestParam("symbols", required = false) symbols: String?,
        @RequestParam("field", required = false) fieldName: String?,
        @RequestParam("direction", required = false) direction: String?,
        @RequestParam("folderId", required = false) folderId: String?,
    ): GetAnalyzersResponse {
        val (total, active, notActive) = analyzerService.getAnalyzersCounters(
            authentication.name,
            folderId,
            symbols?.split(",")?.toList() ?: emptyList()
        )
        return GetAnalyzersResponse(
            analyzerService.getAnalyzers(
                authentication.name,
                page,
                size,
                status,
                symbols,
                fieldName,
                direction,
                folderId
            ),
            total,
            active,
            notActive
        )
    }


    @GetMapping("/{analyzerId}")
    fun getAnalyzer(
        authentication: Authentication,
        @PathVariable analyzerId: String,
    ): ResponseEntity<GridTableAnalyzerResponse> =
        try {
            ResponseEntity(analyzerService.getAnalyzer(analyzerId, authentication.name), HttpStatus.OK)
        } catch (ex: AnalyzerNotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }

    @DeleteMapping
    fun deleteAnalyzers(authentication: Authentication, @RequestBody request: IdListRequest): ResponseEntity<Unit> =
        try {
            analyzerService.deleteAnalyzers(authentication.name, request.ids)
            ResponseEntity(HttpStatus.OK)
        } catch (ex: AnalyzerNotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    fun createAnalyzer(authentication: Authentication, @RequestBody request: CreateAnalyzerRequest) =
        analyzerService.createAnalyzer(authentication.name, request)

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.OK)
    fun bulkCreateAnalyzers(user: Authentication, @RequestBody request: CreateAnalyzerBulkRequest) =
        analyzerService.bulkCreate(user.name, request)

    @PutMapping("/activate/bulk")
    fun activateAnalyzers(authentication: Authentication, @RequestBody request: IdListRequest): ResponseEntity<Unit> =
        try {
            analyzerService.updateAnalyzersStatus(authentication.name, request.ids, true)
            ResponseEntity(HttpStatus.OK)
        } catch (ex: AnalyzerNotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }

    @PutMapping("/deactivate/bulk")
    fun deactivateAnalyzers(authentication: Authentication, @RequestBody request: IdListRequest): ResponseEntity<Unit> =
        try {
            analyzerService.updateAnalyzersStatus(authentication.name, request.ids, false)
            ResponseEntity(HttpStatus.OK)
        } catch (ex: AnalyzerNotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }

    @PatchMapping("/reset")
    fun resetAnalyzers(authentication: Authentication, @RequestBody request: IdListRequest): ResponseEntity<Unit> =
        try {
            analyzerService.resetAnalyzers(authentication.name, request.ids)
            ResponseEntity(HttpStatus.OK)
        } catch (ex: AnalyzerNotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
}
