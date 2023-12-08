package space.dawdawich.services

import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import kotlin.properties.Delegates

class PriceTickerListener(private val kafkaContainer: ConcurrentMessageListenerContainer<String, String>) {
    private val observers = mutableListOf<(Double, Double) -> Unit>()

    private var price: Double by Delegates.observable(0.0) { _, oldValue, newValue ->
        observers.parallelStream().forEach { it(if (oldValue == 0.0) newValue else oldValue, newValue) }
    }

    init {
        kafkaContainer.setupMessageListener(MessageListener<String, String> {
            price = it.value().toDouble()
        })
        kafkaContainer.start()
        Runtime.getRuntime().addShutdownHook(Thread {
            if (kafkaContainer.isRunning) {
                kafkaContainer.stop()
            }
        })
    }

    fun stopContainer() {
        if (kafkaContainer.isRunning) {
            kafkaContainer.stop()
        }
    }

    fun addObserver(observer: (Double, Double) -> Unit) {
        observers.add(observer)
    }

    fun removeObserver(observer: (Double, Double) -> Unit) {
        observers.remove(observer)
    }
}
