package space.dawdawich.services

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.kafka.listener.AcknowledgingMessageListener
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.MessageListener

class PriceTickerListener(private val kafkaContainer: ConcurrentMessageListenerContainer<String, String>) {
    private val observers = mutableListOf<(Double) -> Unit>()


    init {
        kafkaContainer.setupMessageListener(AcknowledgingMessageListener<String, String> { record, acknowledge ->
            val newPrice = record.value().toDouble()
            acknowledge?.acknowledge()
            runBlocking {
                observers.forEach { execute -> launch { execute(newPrice) } }
            }
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
