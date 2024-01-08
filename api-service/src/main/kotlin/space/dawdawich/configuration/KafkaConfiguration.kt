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
    open fun analyzerInfoDocumentKafkaListenerContainerFactory(analyzerInfoConsumerFactory: ConsumerFactory<String, GridTableDetailInfoModel>) =
        ConcurrentKafkaListenerContainerFactory<String, GridTableDetailInfoModel>().apply { this.consumerFactory = analyzerInfoConsumerFactory }

    @Bean
    open fun managerInfoDocumentKafkaListenerContainerFactory(managerInfoConsumerFactory: ConsumerFactory<String, ManagerInfoModel>) =
        ConcurrentKafkaListenerContainerFactory<String, ManagerInfoModel>().apply { this.consumerFactory = managerInfoConsumerFactory }

    @Bean
    open fun analyzerInfoConsumerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String) =
        consumerFactory<GridTableDetailInfoModel>(bootstrapServer)

    @Bean
    open fun managerInfoConsumerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String) =
        consumerFactory<ManagerInfoModel>(bootstrapServer)

    @Bean
    open fun kafkaTemplate(factory: ProducerFactory<String, String>): KafkaTemplate<String, String> =
        KafkaTemplate(factory)

    @Bean
    open fun managerDocumentKafkaTemplate(factory: ProducerFactory<String, TradeManagerDocument>): KafkaTemplate<String, TradeManagerDocument> =
        KafkaTemplate(factory)

    @Bean
    open fun producerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String) =
        producerFactory<String>(bootstrapServer, StringSerializer::class.java)

    @Bean
    open fun managerProducerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String) =
        producerFactory<TradeManagerDocument>(bootstrapServer, JsonSerializer::class.java)

    private fun <T> consumerFactory(bootstrapServer: String): ConsumerFactory<String, T> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        configProps[ConsumerConfig.GROUP_ID_CONFIG] = "ticker_group"
        configProps[JsonDeserializer.TRUSTED_PACKAGES] = "*"
        return DefaultKafkaConsumerFactory(configProps)
    }

    private fun <T> producerFactory(bootstrapServer: String, serializerClass: Class<*>): ProducerFactory<String, T> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = serializerClass
        return DefaultKafkaProducerFactory(configProps)
    }
}
