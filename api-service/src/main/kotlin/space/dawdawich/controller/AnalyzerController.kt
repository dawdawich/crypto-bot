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
            @RequestParam("size", defaultValue = "10") size: Int
    ): GetAnalyzersResponse = GetAnalyzersResponse(analyzerService.getAnalyzers(authentication.name, page, size), analyzerService.getAnalyzersCount(authentication.name))


    @GetMapping("/{analyzerId}")
    fun getAnalyzer(@PathVariable analyzerId: String): ResponseEntity<GridTableAnalyzerResponse> =
            try {
                ResponseEntity(analyzerService.getAnalyzer(analyzerId), HttpStatus.OK)
            } catch (ex: AnalyzerNotFoundException) {
                ResponseEntity(HttpStatus.NOT_FOUND)
            }

    @DeleteMapping("/{analyzerId}")
    fun deleteAnalyzer(authentication: Authentication, @PathVariable analyzerId: String): ResponseEntity<Unit> =
            try {
                analyzerService.deleteAnalyzer(authentication.name, analyzerId)
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
    fun bulkCreateAnalyzers(user: Authentication, @RequestBody request: AnalyzerBulkCreateRequest) =
            analyzerService.bulkCreate(user.name, request)

    @PutMapping("/status/all")
    @RolesAllowed("ADMIN")
    @ResponseStatus(HttpStatus.OK)
    fun setAllAnalyzerStatus(user: Authentication, request: ActivationRequest) =
            analyzerService.changeAllAnalyzersStatus(user.name, request.status)

    @DeleteMapping("/all")
    @RolesAllowed("ADMIN")
    @ResponseStatus(HttpStatus.OK)
    fun deleteAllAnalyzers(user: Authentication) = analyzerService.deleteAnalyzers(user.name)

    @PutMapping("/{analyzerId}/activate")
    fun activateAnalyzer(authentication: Authentication, @PathVariable analyzerId: String): ResponseEntity<Unit> =
            try {
                analyzerService.updateAnalyzerStatus(authentication.name, analyzerId, true)
                ResponseEntity(HttpStatus.OK)
            } catch (ex: AnalyzerNotFoundException) {
                ResponseEntity(HttpStatus.NOT_FOUND)
            }

    @PutMapping("/{analyzerId}/deactivate")
    fun deactivateAnalyzer(authentication: Authentication, @PathVariable analyzerId: String): ResponseEntity<Unit> =
            try {
                analyzerService.updateAnalyzerStatus(authentication.name, analyzerId, false)
                ResponseEntity(HttpStatus.OK)
            } catch (ex: AnalyzerNotFoundException) {
                ResponseEntity(HttpStatus.NOT_FOUND)
            }
}
