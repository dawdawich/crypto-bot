package space.dawdawich

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
open class AnalyzerServiceApplication

fun main() {
    runApplication<AnalyzerServiceApplication>()
}
