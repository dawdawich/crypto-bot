package space.dawdawich.configuration

import org.springframework.amqp.core.Queue
import org.springframework.amqp.support.converter.SimpleMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import space.dawdawich.constants.BACK_TEST_SERVICE
import space.dawdawich.constants.PREDEFINED_BACK_TEST_SERVICE

@Configuration
class RabbitConfiguration {

    @Bean
    fun createBacktestTopic() = Queue(BACK_TEST_SERVICE)

    @Bean
    fun createPredefinedBacktestTopic() = Queue(PREDEFINED_BACK_TEST_SERVICE)

    @Bean
    fun converter(): SimpleMessageConverter {
        return SimpleMessageConverter().apply {
            setAllowedListPatterns(listOf("space.dawdawich.*", "java.util.*"))
        }
    }
}
