package space.dawdawich.configuration

import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import space.dawdawich.constants.DEACTIVATE_MANAGER_TOPIC

@Configuration
class RabbitMQConfiguration {

    @Bean
    fun activateManagerTopic() = Queue(space.dawdawich.constants.ACTIVATE_MANAGER_TOPIC)

    @Bean
    fun deactivateManagerTopic() = Queue(DEACTIVATE_MANAGER_TOPIC)
}
