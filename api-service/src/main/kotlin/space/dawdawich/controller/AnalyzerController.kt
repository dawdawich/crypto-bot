package space.dawdawich.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import space.dawdawich.model.GridTableAnalyzer
import space.dawdawich.model.Position
import space.dawdawich.service.AnalyzerService

@RestController
@RequestMapping("/analyzer")
class AnalyzerController(private val analyzerService: AnalyzerService) {

    @GetMapping("/top20")
    fun getTopAnalyzers(): List<GridTableAnalyzer> = analyzerService.getTopAnalyzers()

    @GetMapping("/{analyzerId}")
    fun getAnalyzer(@PathVariable analyzerId: String): GridTableAnalyzer = analyzerService.getAnalyzer(analyzerId)

    @GetMapping("/positions/{analyzerId}")
    fun getCompleteAnalyzerPosition(@PathVariable analyzerId: String): List<Position> = analyzerService.getCompletePositionsForAnalyzer(analyzerId)
}
