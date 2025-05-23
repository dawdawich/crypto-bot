package space.dawdawich.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import space.dawdawich.controller.model.*
import space.dawdawich.controller.model.analyzer.*
import space.dawdawich.exception.AnalyzerLimitExceededException
import space.dawdawich.exception.model.AnalyzerNotFoundException
import space.dawdawich.service.AnalyzerService

@RestController
@RequestMapping("/analyzer")
class AnalyzerController(private val analyzerService: AnalyzerService) {

    @GetMapping("/top20")
    fun getTopAnalyzers() = analyzerService.getTopAnalyzers()

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
    ) = try {
        ResponseEntity(analyzerService.getAnalyzer(analyzerId, authentication.name), HttpStatus.OK)
    } catch (ex: AnalyzerNotFoundException) {
        ResponseEntity(HttpStatus.NOT_FOUND)
    }

    @GetMapping("/active/count")
    fun getActiveAnalyzerCount(authentication: Authentication) =
        ResponseEntity(analyzerService.getActiveAnalyzersCount(authentication.name), HttpStatus.OK)

    @DeleteMapping
    fun bulkDeleteAnalyzers(authentication: Authentication, @RequestBody request: IdListRequest): ResponseEntity<Unit> =
        try {
            analyzerService.deleteAnalyzers(authentication.name, request.ids, request.all)
            ResponseEntity(HttpStatus.OK)
        } catch (ex: AnalyzerNotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }

    @PostMapping
    fun createGridTableAnalyzer(authentication: Authentication, @RequestBody request: CreateAnalyzerRequest) =
        try {
            analyzerService.createAnalyzer(authentication.name, request)
            ResponseEntity<Unit>(HttpStatus.OK)
        } catch (ex: AnalyzerLimitExceededException) {
            ResponseEntity<Unit>(HttpStatus.PAYMENT_REQUIRED)
        }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.OK)
    fun bulkCreateAnalyzers(user: Authentication, @RequestBody request: CreateAnalyzerBulkRequest) =
        try {
            analyzerService.bulkCreate(user.name, request)
            ResponseEntity<Unit>(HttpStatus.OK)
        } catch (ex: AnalyzerLimitExceededException) {
            ResponseEntity<Unit>(HttpStatus.PAYMENT_REQUIRED)
        }

    @PutMapping("/activate/bulk")
    fun activateAnalyzers(authentication: Authentication, @RequestBody request: IdListRequest): ResponseEntity<Unit> =
        try {
            analyzerService.updateAnalyzersStatus(authentication.name, true, request.ids, request.all)
            ResponseEntity(HttpStatus.OK)
        } catch (ex: AnalyzerNotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } catch (ex: AnalyzerLimitExceededException) {
            ResponseEntity<Unit>(HttpStatus.PAYMENT_REQUIRED)
        }

    @PutMapping("/deactivate/bulk")
    fun deactivateAnalyzers(authentication: Authentication, @RequestBody request: IdListRequest): ResponseEntity<Unit> =
        try {
            analyzerService.updateAnalyzersStatus(authentication.name, false, request.ids, request.all)
            ResponseEntity(HttpStatus.OK)
        } catch (ex: AnalyzerNotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }

    @PatchMapping("/reset")
    fun resetAnalyzers(authentication: Authentication, @RequestBody request: IdListRequest): ResponseEntity<Unit> =
        try {
            analyzerService.resetAnalyzers(authentication.name, request.ids, request.all)
            ResponseEntity(HttpStatus.OK)
        } catch (ex: AnalyzerNotFoundException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
}
