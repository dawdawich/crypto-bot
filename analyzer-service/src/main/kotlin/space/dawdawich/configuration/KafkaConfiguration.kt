package space.dawdawich.configuration

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonSerializer
import java.util.*
import kotlin.collections.HashMap

@Configuration
class KafkaConfiguration {

    @Bean
    fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, String>) =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            this.consumerFactory = consumerFactory
            this.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        }

    @Bean
    fun <T> kafkaListenerReplayingContainerFactory(consumerFactory: ConsumerFactory<String, String>, jsonKafkaTemplate: KafkaTemplate<String, T>) =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            this.consumerFactory = consumerFactory
            setReplyTemplate(jsonKafkaTemplate)
        }

    @Bean
    fun consumerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String): ConsumerFactory<String, String> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.GROUP_ID_CONFIG] = "analyzer_ticker_group"
        configProps[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        configProps[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = 25000
        configProps[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] = 20000
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
