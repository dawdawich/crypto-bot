package space.dawdawich.service

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.BYBIT_TICKER_TOPIC

@Service
class KafkaManager(private val kafkaTemplate: KafkaTemplate<String, String>) {

    fun sendTickerEvent(topic: String, symbol: String, partition: Int, value: String) {
        kafkaTemplate.send(topic, partition, symbol, value)
    }
}
