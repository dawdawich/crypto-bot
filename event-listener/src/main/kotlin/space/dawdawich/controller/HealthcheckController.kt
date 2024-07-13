package space.dawdawich.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import space.dawdawich.service.RabbitManager

@RestController
class HealthcheckController(private val rabbitManager: RabbitManager) {

    @GetMapping("/topics-update-time")
    fun checkHealth() = rabbitManager.lastUpdateTopicTime
}
