package space.dawdawich.service

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.BYBIT_TICKER_TOPIC

@Service
class KafkaManager(private val kafkaTemplate: KafkaTemplate<String, String>) {

    val lastUpdateTopicTime: MutableMap<String, Long> = mutableMapOf()

    fun sendTickerEvent(topic: String, symbol: String, partition: Int, value: String) {
        lastUpdateTopicTime["$topic-$partition"] = System.currentTimeMillis()
        kafkaTemplate.send(topic, partition, symbol, value)
    }
}
