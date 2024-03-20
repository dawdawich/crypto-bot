package space.dawdawich.service

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.seconds

@Service
class KafkaManager(private val kafkaTemplate: KafkaTemplate<String, String>) {

    val lastUpdateTopicTime: MutableMap<String, Long> = mutableMapOf()

    fun sendTickerEvent(topic: String, symbol: String, partition: Int, value: String) {
        val currentTime = System.currentTimeMillis()
        if ((currentTime - (lastUpdateTopicTime["$topic-$partition"] ?: 0)) > 5.seconds.inWholeMilliseconds) {
            lastUpdateTopicTime["$topic-$partition"] = System.currentTimeMillis()
            kafkaTemplate.send(topic, partition, symbol, value)
        }
    }
}
