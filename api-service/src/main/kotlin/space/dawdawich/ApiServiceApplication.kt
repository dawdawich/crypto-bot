package space.dawdawich

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.socket.config.annotation.EnableWebSocket

@EnableWebMvc
@EnableMongoRepositories
@EnableReactiveMongoRepositories
@SpringBootApplication
open class ApiServiceApplication

fun main() {
    runApplication<ApiServiceApplication>()
}
