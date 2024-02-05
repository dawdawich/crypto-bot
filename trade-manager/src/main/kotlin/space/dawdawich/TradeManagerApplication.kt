package space.dawdawich

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@EnableMongoRepositories
@SpringBootApplication
@EnableWebMvc
class TradeManagerApplication

fun main() {
    runApplication<TradeManagerApplication>()
}
