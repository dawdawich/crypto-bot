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
import space.dawdawich.constants.RESPONSE_ANALYZER_RUNTIME_DATA
import space.dawdawich.model.strategy.AnalyzerRuntimeInfoModel
import space.dawdawich.repositories.mongo.entity.TradeManagerDocument

@Configuration
class KafkaConfiguration {

    /**
     * Creates a Kafka listener container factory that consumes JSON messages.
     *
     * @param managerInfoConsumerFactory The consumer factory for deserializing JSON messages.
     * @return The Kafka listener container factory.
     */
    @Bean
    fun <T> jsonKafkaListenerContainerFactory(managerInfoConsumerFactory: ConsumerFactory<String, T>) =
        ConcurrentKafkaListenerContainerFactory<String, T>().apply { this.consumerFactory = managerInfoConsumerFactory }

    /**
     * Creates a JSON consumer factory for Kafka.
     *
     * @param bootstrapServer The bootstrap server for the Kafka cluster.
     * @return The JSON consumer factory.
     */
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

    /**
     * Creates a Kafka template for producing messages to Kafka.
     *
     * @param factory The producer factory to be used by the Kafka template.
     * @return The Kafka template.
     */
    @Bean
    fun kafkaTemplate(factory: ProducerFactory<String, String>): KafkaTemplate<String, String> =
        KafkaTemplate(factory)

    /**
     * Creates a Kafka template for producing messages to Kafka.
     *
     * @param jsonProducerFactory The producer factory to be used by the Kafka template.
     * @return The Kafka template.
     */
    @Bean
    fun <T> jsonKafkaTemplate(jsonProducerFactory: ProducerFactory<String, T>): KafkaTemplate<String, T> =
        KafkaTemplate(jsonProducerFactory)

    /**
     * Creates a producer factory for Kafka.
     *
     * @param bootstrapServer The bootstrap server for the Kafka cluster.
     * @return The Kafka producer factory.
     */
    @Bean
    fun producerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String) =
        producerFactory<String>(bootstrapServer, StringSerializer::class.java)

    /**
     * Creates a JSON producer factory for Kafka.
     *
     * @param bootstrapServer The bootstrap server for the Kafka cluster.
     * @return The JSON producer factory.
     */
    @Bean
    fun jsonProducerFactory(@Value("\${spring.kafka.bootstrap-servers}") bootstrapServer: String) =
        producerFactory<TradeManagerDocument>(bootstrapServer, JsonSerializer::class.java)

    /**
     * Creates a replying Kafka template for sending and receiving messages to and from Kafka for
     * [RESPONSE_ANALYZER_RUNTIME_DATA].
     *
     * @param producerFactory The producer factory used by the Kafka template.
     * @param jsonKafkaListenerContainerFactory The Kafka listener container factory used by the Kafka template.
     * @return The replying Kafka template.
     */
    @Bean
    fun strategyRuntimeDataReplyingTemplate(
        producerFactory: ProducerFactory<String, String>,
        jsonKafkaListenerContainerFactory: ConcurrentKafkaListenerContainerFactory<String, AnalyzerRuntimeInfoModel?>
    ) = replyingTemplate(
        producerFactory,
        jsonKafkaListenerContainerFactory,
        RESPONSE_ANALYZER_RUNTIME_DATA
    ).apply { setSharedReplyTopic(true) }

    /**
     * Creates a producer factory for Kafka.
     *
     * @param bootstrapServer The bootstrap server for the Kafka cluster.
     * @param serializerClass The serializer class for the Kafka messages.
     * @return The Kafka producer factory.
     * @param T The type of the Kafka message value.
     */
    private fun <T> producerFactory(bootstrapServer: String, serializerClass: Class<*>): ProducerFactory<String, T> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServer
        configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = serializerClass
        return DefaultKafkaProducerFactory(configProps)
    }

    /**
     * Creates a replying Kafka template for sending and receiving messages to and from Kafka.
     *
     * @param producerFactory The producer factory used by the Kafka template.
     * @param jsonKafkaListenerContainerFactory The Kafka listener container factory used by the Kafka template.
     * @param topic The Kafka topic to which the template will send messages.
     * @return The replying Kafka template.
     * @param T The type of the Kafka message value.
     */
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
