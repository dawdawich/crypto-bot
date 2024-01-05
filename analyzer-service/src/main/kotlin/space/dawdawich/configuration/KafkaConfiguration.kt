package space.dawdawich.configuration

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer
import space.dawdawich.repositories.entity.TradeManagerDocument

@Configuration
open class KafkaConfiguration {

    @Bean
    open fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, String>) =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply { this.consumerFactory = consumerFactory }

    @Bean
    open fun consumerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String): ConsumerFactory<String, String> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configProps[ConsumerConfig.GROUP_ID_CONFIG] = "ticker_group"
        configProps[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = "5000"
        return DefaultKafkaConsumerFactory(configProps)
    }
}
