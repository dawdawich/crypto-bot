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
import space.dawdawich.constants.RESPONSE_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC
import space.dawdawich.model.strategy.StrategyRuntimeInfoModel
import space.dawdawich.repositories.entity.TradeManagerDocument

@Configuration
class KafkaConfiguration {

    @Bean
    fun <T> jsonKafkaListenerContainerFactory(managerInfoConsumerFactory: ConsumerFactory<String, T>) =
        ConcurrentKafkaListenerContainerFactory<String, T>().apply { this.consumerFactory = managerInfoConsumerFactory }

    @Bean
    fun <T> jsonConsumerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String): ConsumerFactory<String, T> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        configProps[ConsumerConfig.GROUP_ID_CONFIG] = "api-service-group"
        configProps[JsonDeserializer.TRUSTED_PACKAGES] = "*"
        return DefaultKafkaConsumerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(factory: ProducerFactory<String, String>): KafkaTemplate<String, String> =
        KafkaTemplate(factory)

    @Bean
    fun <T> jsonKafkaTemplate(jsonProducerFactory: ProducerFactory<String, T>): KafkaTemplate<String, T> =
        KafkaTemplate(jsonProducerFactory)

    @Bean
    fun producerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String) =
        producerFactory<String>(bootstrapServer, StringSerializer::class.java)

    @Bean
    fun jsonProducerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String) =
        producerFactory<TradeManagerDocument>(bootstrapServer, JsonSerializer::class.java)

    @Bean
    fun strategyRuntimeDataReplyingTemplate(
        producerFactory: ProducerFactory<String, String>,
        jsonKafkaListenerContainerFactory: ConcurrentKafkaListenerContainerFactory<String, StrategyRuntimeInfoModel>
    ) = replyingTemplate(producerFactory, jsonKafkaListenerContainerFactory, RESPONSE_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC)

    private fun <T> producerFactory(bootstrapServer: String, serializerClass: Class<*>): ProducerFactory<String, T> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = serializerClass
        return DefaultKafkaProducerFactory(configProps)
    }

    private fun <T> replyingTemplate(
        producerFactory: ProducerFactory<String, String>,
        jsonKafkaListenerContainerFactory: ConcurrentKafkaListenerContainerFactory<String, T>,
        topic: String
    ): ReplyingKafkaTemplate<String, String, T> {
        val replyContainer =
            jsonKafkaListenerContainerFactory.createContainer(topic).apply {
                isAutoStartup = false
            }
        return ReplyingKafkaTemplate(producerFactory, replyContainer)
    }
}
