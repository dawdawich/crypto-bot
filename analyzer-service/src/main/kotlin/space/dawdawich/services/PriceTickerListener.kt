package space.dawdawich.services

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.kafka.listener.AcknowledgingMessageListener
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.MessageListener

class PriceTickerListener(private val kafkaContainer: ConcurrentMessageListenerContainer<String, String>) {
    private val log = KotlinLogging.logger {  }
    private val observers = mutableListOf<(Double) -> Unit>()

    init {
        kafkaContainer.setupMessageListener(AcknowledgingMessageListener<String, String> { record, acknowledge ->
            val startTime = System.currentTimeMillis()
            val newPrice = record.value().toDouble()
            acknowledge?.acknowledge()
            runBlocking {
                observers.forEach { execute -> launch { execute(newPrice) } }
            }
            log.info { "Processed price update for symbol '${record.key()}'; topic '${record.topic()}'; time elapsed - '${System.currentTimeMillis() - startTime}'" }
        })
        kafkaContainer.start()
        Runtime.getRuntime().addShutdownHook(Thread {
            if (kafkaContainer.isRunning) {
                kafkaContainer.stop()
            }
        })
    }

    fun addObserver(observer: (Double) -> Unit) {
        observers.add(observer)
    }

    fun removeObserver(observer: (Double) -> Unit) {
        observers.remove(observer)
    }
}
