package space.dawdawich.configuration

import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import space.dawdawich.constants.BACK_TEST_SERVICE

@Configuration
class RabbitConfiguration {

    @Bean
    fun createBacktestTopic() = Queue(BACK_TEST_SERVICE)
}
