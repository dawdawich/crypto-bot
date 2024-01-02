package space.dawdawich.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import space.dawdawich.service.KafkaManager

@RestController
class HealthcheckController(private val kafkaManager: KafkaManager) {

    @GetMapping("/topics-update-time")
    fun checkHealth() = kafkaManager.lastUpdateTopicTime
}
