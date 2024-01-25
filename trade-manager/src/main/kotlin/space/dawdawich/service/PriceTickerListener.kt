package space.dawdawich.service

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.listener.MessageListener

class PriceTickerListener(private val kafkaContainer: ConcurrentMessageListenerContainer<String, String>) {
    private val observers = mutableListOf<(Double) -> Unit>()


    init {
        kafkaContainer.setupMessageListener(MessageListener<String, String> {
            runBlocking {
                observers.forEach { execute -> launch { execute(it.value().toDouble()) } }
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
