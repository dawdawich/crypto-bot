package space.dawdawich

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@EnableMongoRepositories
@EnableReactiveMongoRepositories
@SpringBootApplication
open class TradeManagerApplication

fun main() {
    runApplication<TradeManagerApplication>()

    while (true) {

    }
}
