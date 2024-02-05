package space.dawdawich

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@EnableWebMvc
@EnableMongoRepositories
@SpringBootApplication
class ApiServiceApplication

fun main() {
    runApplication<ApiServiceApplication>()
}
