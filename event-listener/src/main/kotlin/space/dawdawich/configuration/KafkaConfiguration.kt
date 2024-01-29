package space.dawdawich.configuration

import org.apache.kafka.clients.admin.AdminClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@EnableKafka
@Configuration
class KafkaConfiguration {

    @Bean
    fun kafkaTemplate(factory: ProducerFactory<String, String>): KafkaTemplate<String, String> {
        return KafkaTemplate(factory)
    }

    @Bean
    fun adminClient(@Value("\${spring.kafka.bootstrap-servers}") hostAddress: String): AdminClient = AdminClient.create(
        mapOf("bootstrap.servers" to hostAddress)
    )
}
