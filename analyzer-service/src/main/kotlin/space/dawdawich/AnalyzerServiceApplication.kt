package space.dawdawich

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@EnableWebMvc
@EnableScheduling
@SpringBootApplication
class AnalyzerServiceApplication

fun main() {
    runApplication<AnalyzerServiceApplication>()
}
