package space.dawdawich.config

import org.springframework.amqp.support.converter.SimpleMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMqConfiguration {

    @Bean
    fun converter(): SimpleMessageConverter {
        return SimpleMessageConverter().apply {
            setAllowedListPatterns(listOf("space.dawdawich.*", "java.util.*"))
        }
    }
}
