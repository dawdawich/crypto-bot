package space.dawdawich.service

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.seconds

@Service
class RabbitManager(private val rabbitTemplate: RabbitTemplate) {
    val lastUpdateTopicTime: MutableMap<String, Long> = mutableMapOf()

    fun sendTickerEvent(topic: String, symbol: String, value: Double) {
        val currentTime = System.currentTimeMillis()
        if ((currentTime - (lastUpdateTopicTime["$topic-$symbol"] ?: 0)) > 10.seconds.inWholeMilliseconds) {
            lastUpdateTopicTime["$topic-$symbol"] = System.currentTimeMillis()
            rabbitTemplate.convertAndSend(topic, symbol, value)
        }
    }

    fun sendKLineEvent(topic: String, symbol: String, value: Any) = rabbitTemplate.convertAndSend(topic, symbol, value)
}
