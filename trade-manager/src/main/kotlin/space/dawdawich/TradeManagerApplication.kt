package space.dawdawich

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@EnableMongoRepositories
@EnableReactiveMongoRepositories
@SpringBootApplication
@EnableWebMvc
open class TradeManagerApplication

fun main() {
    runApplication<TradeManagerApplication>()
}
