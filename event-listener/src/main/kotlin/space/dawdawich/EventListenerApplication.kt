package space.dawdawich

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication(exclude = [RedisAutoConfiguration::class])
class EventListenerApplication

fun main() {
    runApplication<EventListenerApplication>()
}
