package space.dawdawich

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.EnableWebMvc

/**
 * This class represents the main entry point for the API service application.
 *
 * It is annotated with the Spring Boot annotations [@EnableWebMvc], [@EnableScheduling] and [@EnableMongoRepositories].
 *
 * [@EnableWebMvc] enables Spring MVC configuration for the application.
 * [@EnableScheduling] enables scheduling support in the application.
 * [@EnableMongoRepositories] enables Spring Data MongoDB repositories for the application.
 *
 * This class requires Spring Boot to run, so it is also annotated with [@SpringBootApplication].
 * [@SpringBootApplication] is a convenient annotation that combines [@Configuration],
 * [@EnableAutoConfiguration] and [@ComponentScan].
 *
 * To run the application, you need to call the static function [runApplication] and pass [ApiServiceApplication] as an argument.
 */
@EnableWebMvc
@EnableScheduling
@EnableMongoRepositories
@SpringBootApplication
class ApiServiceApplication

fun main() {
    runApplication<ApiServiceApplication>()
}
