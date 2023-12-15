package space.dawdawich.controller

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import space.dawdawich.controller.model.CreateAnalyzerRequest
import space.dawdawich.controller.model.GridTableAnalyzerResponse
import space.dawdawich.service.AnalyzerService

@RestController
@RequestMapping("/analyzer")
class AnalyzerController(private val analyzerService: AnalyzerService) {

    @GetMapping("/top20")
    fun getTopAnalyzers(): List<GridTableAnalyzerResponse> = analyzerService.getTopAnalyzers()

    @GetMapping
    fun getAnalyzers(authentication: Authentication) = analyzerService.getAnalyzers(authentication.name)

    @GetMapping("/{analyzerId}")
    fun getAnalyzer(@PathVariable analyzerId: String): GridTableAnalyzerResponse = analyzerService.getAnalyzer(analyzerId) // TODO: Add check for UUID

    @DeleteMapping("/{analyzerId}")
    fun deleteAnalyzer(@PathVariable analyzerId: String) = analyzerService.deleteAnalyzer(analyzerId)

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    fun createAnalyzer(authentication: Authentication, @RequestBody request: CreateAnalyzerRequest) {
        analyzerService.createAnalyzer(authentication.name, request)
    }

    @PutMapping("/{analyzerId}/activate")
    @ResponseStatus(HttpStatus.OK)
    fun activateAnalyzer(authentication: Authentication, @PathVariable analyzerId: String) = analyzerService.updateAnalyzerStatus(authentication.name, analyzerId, true)

    @PutMapping("/{analyzerId}/deactivate")
    @ResponseStatus(HttpStatus.OK)
    fun deactivateAnalyzer(authentication: Authentication, @PathVariable analyzerId: String) = analyzerService.updateAnalyzerStatus(authentication.name, analyzerId, false)
}
