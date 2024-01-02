package space.dawdawich.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate

@RestController
@RequestMapping("/health-check")
class HealthCheckController(
    restTemplateBuilder: RestTemplateBuilder,
    @Value("\${app.services-url}") private val servicesListConfig: String
) {

    private val restTemplate: RestTemplate = restTemplateBuilder.build()

    @GetMapping
    fun healthCheck() = servicesListConfig.split(",").associate {
        val (key, url) = it.split("=")

        key to runCatching { restTemplate.getForObject(url, String::class.java) ?: "{\"status\":\"DOWN\"}" }.getOrElse {
            "{\"status\":\"DOWN\"}"
        }
    }

}
