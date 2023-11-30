package space.dawdawich.service

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.TICKER_TOPIC

@Service
class KafkaManager(private val kafkaTemplate: KafkaTemplate<String, String>) {

    fun sendTickerEvent(symbol: String, partition: Int, value: String) {
        kafkaTemplate.send(TICKER_TOPIC, partition, symbol, value)
    }
}
