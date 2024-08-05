package space.dawdawich.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import space.dawdawich.constants.BYBIT_KLINE_TOPIC
import space.dawdawich.constants.BYBIT_TEST_KLINE_TOPIC
import space.dawdawich.constants.BYBIT_TEST_TICKER_TOPIC
import space.dawdawich.constants.BYBIT_TICKER_TOPIC

@EnableRabbit
@Configuration
class RabbitMQConfiguration {

    @Bean
    fun klineTopicExchange() = TopicExchange(BYBIT_KLINE_TOPIC)

    @Bean
    fun klineDemoTopicExchange() = TopicExchange(BYBIT_TEST_KLINE_TOPIC)

    @Bean
    fun tickerTopicExchange() = TopicExchange(BYBIT_TICKER_TOPIC)

    @Bean
    fun tickerDemoTopicExchange() = TopicExchange(BYBIT_TEST_TICKER_TOPIC)

    @Bean
    fun tickerWithRSITopicExchange() = TopicExchange("tickerWithRSI")

    @Bean
    fun jsonMessageConverter(): Jackson2JsonMessageConverter =
        Jackson2JsonMessageConverter(ObjectMapper().findAndRegisterModules())

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory, jsonMessageConverter: Jackson2JsonMessageConverter): RabbitTemplate {
        return RabbitTemplate(connectionFactory).apply { messageConverter = jsonMessageConverter }
    }
}
