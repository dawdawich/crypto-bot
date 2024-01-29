package space.dawdawich.configuration

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import space.dawdawich.constants.RESPONSE_ANALYZER_STRATEGY_CONFIG_TOPIC
import space.dawdawich.model.manager.ManagerInfoModel
import space.dawdawich.model.strategy.StrategyConfigModel
import space.dawdawich.repositories.entity.TradeManagerDocument


@Configuration
class KafkaConfiguration {

    @Bean
    fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, String>) =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply { this.consumerFactory = consumerFactory }

    @Bean
    fun <T> jsonKafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, T>) =
        ConcurrentKafkaListenerContainerFactory<String, T>().apply { this.consumerFactory = consumerFactory }

    @Bean
    fun consumerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String): ConsumerFactory<String, String> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.GROUP_ID_CONFIG] = "manager_ticker_group"
        return DefaultKafkaConsumerFactory(configProps)
    }

    @Bean
    fun managerConsumerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String): ConsumerFactory<String, TradeManagerDocument> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
//        configProps[ConsumerConfig.GROUP_ID_CONFIG] = "ticker_group"
        configProps[JsonDeserializer.TRUSTED_PACKAGES] = "*"
        return DefaultKafkaConsumerFactory(configProps)
    }

    @Bean
    fun managerInfoProducerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String): ProducerFactory<String, ManagerInfoModel> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun managerInfoKafkaTemplate(factory: ProducerFactory<String, ManagerInfoModel>): KafkaTemplate<String, ManagerInfoModel> =
        KafkaTemplate(factory)

    @Bean
    fun strategyConfigReplyingTemplate(factory: ProducerFactory<String, String>, jsonKafkaListenerContainerFactory: ConcurrentKafkaListenerContainerFactory<String, StrategyConfigModel>): ReplyingKafkaTemplate<String, String, StrategyConfigModel?> {
        return ReplyingKafkaTemplate(factory, jsonKafkaListenerContainerFactory.createContainer(RESPONSE_ANALYZER_STRATEGY_CONFIG_TOPIC).apply {
            isAutoStartup = false
        })
    }
}
