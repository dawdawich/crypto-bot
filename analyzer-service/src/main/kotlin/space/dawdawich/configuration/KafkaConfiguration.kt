package space.dawdawich.configuration

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class KafkaConfiguration {

    @Bean
    fun kafkaListenerContainerFactory(acknowledgingConsumerFactory: ConsumerFactory<String, String>) =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            this.consumerFactory = acknowledgingConsumerFactory
            this.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        }

    @Bean
    fun <T> kafkaListenerReplayingContainerFactory(consumerFactory: ConsumerFactory<String, String>, jsonKafkaTemplate: KafkaTemplate<String, T>) =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            this.consumerFactory = consumerFactory
            setReplyTemplate(jsonKafkaTemplate)
        }

    @Bean
    fun <T> jsonKafkaListenerReplayingContainerFactory(jsonConsumerFactory: ConsumerFactory<String, T>, jsonKafkaTemplate: KafkaTemplate<String, T>) =
        ConcurrentKafkaListenerContainerFactory<String, T>().apply {
            this.consumerFactory = jsonConsumerFactory
            setReplyTemplate(jsonKafkaTemplate)
        }

    @Bean
    fun consumerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String): ConsumerFactory<String, String> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
        configProps[ConsumerConfig.GROUP_ID_CONFIG] = "analyzer_event_group"
        return DefaultKafkaConsumerFactory(configProps)
    }

    @Bean
    fun <T> jsonConsumerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String): ConsumerFactory<String, T> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        configProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
        configProps[ConsumerConfig.GROUP_ID_CONFIG] = "analyzer_event_group"
        return DefaultKafkaConsumerFactory(configProps)
    }

    @Bean
    fun acknowledgingConsumerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String): ConsumerFactory<String, String> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.GROUP_ID_CONFIG] = "analyzer_ticker_group"
        configProps[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        return DefaultKafkaConsumerFactory(configProps)
    }

    @Bean
    fun <T> jsonKafkaTemplate(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String): KafkaTemplate<String, T> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java

        return KafkaTemplate(DefaultKafkaProducerFactory(configProps))
    }
}
