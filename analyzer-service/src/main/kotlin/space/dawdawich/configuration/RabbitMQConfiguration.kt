package space.dawdawich.configuration

import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import space.dawdawich.constants.*

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
    fun deactivateAnalyzerTopic() = Queue(DEACTIVATE_ANALYZER_TOPIC, false)

    @Bean
    fun activateAnalyzerTopic() = Queue(ACTIVATE_ANALYZER_TOPIC, false)

    @Bean
    fun activateAnalyzersTopic() = Queue(ACTIVATE_ANALYZERS_TOPIC, false)

    @Bean
    fun deleteAnalyzerTopic() = Queue(DELETE_ANALYZER_TOPIC, false)

    @Bean
    fun requestAnalyzerRuntimeData() = Queue(REQUEST_ANALYZER_RUNTIME_DATA, false)

    @Bean
    fun requestAnalyzerStrategyRuntimeDataTopic() = Queue(REQUEST_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC, false)

    @Bean
    fun requestProfitableAnalyzerStrategyConfigTopic() = Queue(REQUEST_PROFITABLE_ANALYZER_STRATEGY_CONFIG_TOPIC, false)
}
