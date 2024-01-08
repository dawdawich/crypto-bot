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
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import space.dawdawich.model.analyzer.GridTableDetailInfoModel
import space.dawdawich.model.manager.ManagerInfoModel
import space.dawdawich.repositories.entity.TradeManagerDocument

@Configuration
open class KafkaConfiguration {

    @Bean
    open fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, String>) =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply { this.consumerFactory = consumerFactory }

    @Bean
    open fun managerDocumentKafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, TradeManagerDocument>) =
        ConcurrentKafkaListenerContainerFactory<String, TradeManagerDocument>().apply { this.consumerFactory = consumerFactory }

    @Bean
    open fun consumerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String): ConsumerFactory<String, String> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.GROUP_ID_CONFIG] = "ticker_group"
        return DefaultKafkaConsumerFactory(configProps)
    }

    @Bean
    open fun managerConsumerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String): ConsumerFactory<String, TradeManagerDocument> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        configProps[ConsumerConfig.GROUP_ID_CONFIG] = "ticker_group"
        configProps[JsonDeserializer.TRUSTED_PACKAGES] = "*"
        return DefaultKafkaConsumerFactory(configProps)
    }

    @Bean
    open fun managerInfoProducerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String): ProducerFactory<String, ManagerInfoModel> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    open fun managerInfoKafkaTemplate(factory: ProducerFactory<String, ManagerInfoModel>): KafkaTemplate<String, ManagerInfoModel> =
        KafkaTemplate(factory)
}
