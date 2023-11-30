package space.dawdawich.configuration

import org.apache.kafka.clients.CommonClientConfigs.GROUP_ID_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerConfig.*
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import space.dawdawich.constants.TICKER_GROUP_ID
import space.dawdawich.constants.TICKER_TOPIC
import space.dawdawich.repositories.SymbolRepository

@Configuration
open class KafkaConfiguration {

    @Bean
    open fun producerFactory(): ProducerFactory<String, String> {
        val kafkaProperties = HashMap<String, Any>()
        kafkaProperties[BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
        kafkaProperties[GROUP_ID_CONFIG] = TICKER_GROUP_ID
        kafkaProperties[KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        kafkaProperties[VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        return DefaultKafkaProducerFactory(kafkaProperties)
    }

    @Bean
    open fun kafkaTemplate(factory: ProducerFactory<String, String>): KafkaTemplate<String, String> {
        return KafkaTemplate(factory)
    }

    @Bean
    open fun kafkaTopics(symbolRepository: SymbolRepository) = KafkaAdmin.NewTopics(
        TopicBuilder.name(TICKER_TOPIC).partitions(symbolRepository.count().toInt())
            .build()
    )
}
