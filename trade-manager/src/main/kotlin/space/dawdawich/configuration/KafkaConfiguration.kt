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
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import space.dawdawich.constants.RESPONSE_ANALYZER_STRATEGY_CONFIG_TOPIC
import space.dawdawich.constants.RESPONSE_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC
import space.dawdawich.model.manager.ManagerInfoModel
import space.dawdawich.model.strategy.StrategyConfigModel
import space.dawdawich.model.strategy.StrategyRuntimeInfoModel
import space.dawdawich.repositories.entity.TradeManagerDocument


@Configuration
class KafkaConfiguration {

    @Bean
    fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, String>) =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            this.consumerFactory = consumerFactory
            this.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        }

    @Bean
    fun <T> jsonKafkaListenerContainerFactory(jsonConsumerFactory: ConsumerFactory<String, T>) =
        ConcurrentKafkaListenerContainerFactory<String, T>().apply { this.consumerFactory = jsonConsumerFactory }

    @Bean
    fun consumerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String): ConsumerFactory<String, String> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.GROUP_ID_CONFIG] = "manager_ticker_group"
        configProps[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        return DefaultKafkaConsumerFactory(configProps)
    }

    @Bean
    fun <T> jsonConsumerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String): ConsumerFactory<String, T> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        configProps[ConsumerConfig.GROUP_ID_CONFIG] = "manager_group"
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
    fun producerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String): ProducerFactory<String, String> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun managerInfoKafkaTemplate(managerInfoProducerFactory: ProducerFactory<String, ManagerInfoModel>): KafkaTemplate<String, ManagerInfoModel> =
        KafkaTemplate(managerInfoProducerFactory)

    @Bean
    fun strategyConfigReplyingTemplate(
        producerFactory: ProducerFactory<String, String>,
        jsonKafkaListenerContainerFactory: ConcurrentKafkaListenerContainerFactory<String, StrategyConfigModel>
    ) =
        replyingTemplate(producerFactory, jsonKafkaListenerContainerFactory, RESPONSE_ANALYZER_STRATEGY_CONFIG_TOPIC)

    @Bean
    fun strategyRuntimeDataReplyingTemplate(
        producerFactory: ProducerFactory<String, String>,
        jsonKafkaListenerContainerFactory: ConcurrentKafkaListenerContainerFactory<String, StrategyRuntimeInfoModel>
    ) = replyingTemplate(producerFactory, jsonKafkaListenerContainerFactory, RESPONSE_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC)

    private fun <T> replyingTemplate(
        producerFactory: ProducerFactory<String, String>,
        jsonKafkaListenerContainerFactory: ConcurrentKafkaListenerContainerFactory<String, T>,
        topic: String
    ): ReplyingKafkaTemplate<String, String, T> {
        val replyContainer =
            jsonKafkaListenerContainerFactory.createContainer(topic).apply {
                isAutoStartup = false
            }
        return ReplyingKafkaTemplate(producerFactory, replyContainer).apply { setSharedReplyTopic(true) }
    }
}
