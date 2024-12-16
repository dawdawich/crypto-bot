package space.dawdawich.services

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer

/**
 * A generic event listener class that subscribes to messages on a specific topic and queue
 * using a provided connection factory. It listens for events of type `T` and notifies registered
 * observers when an event is received.
 *
 * @param T The type of handlers that will process the event.
 * @param connectionFactory The factory used to establish a connection to the message broker.
 * @param topicName The name of the topic to subscribe to.
 * @param queueName The name of the queue to subscribe to the topic.
 * @param typeRef A type reference for deserialization of the message body into an object of type `T`.
 */
class EventListener<T>(connectionFactory: ConnectionFactory, topicName: String, queueName: String, typeRef : TypeReference<T>) {
    private val log = KotlinLogging.logger { }
    private val observers = mutableListOf<(T) -> Unit>()

    init {
        connectionFactory.createConnection().use {
            it.createChannel(false).use { channel ->
                val queue = channel.queueDeclare("$topicName.$queueName", false, false, true, emptyMap()).queue
                channel.queueBind(queue, topicName, queueName)

                val container = SimpleMessageListenerContainer(connectionFactory)
                container.setQueueNames(queue)
                val mapper = jacksonObjectMapper()

                container.setMessageListener { message ->
                    val startTime = System.currentTimeMillis()
                    val event = mapper.readValue(message.body, typeRef)
                    runBlocking {
                        // process event in parallel
                        observers.forEach { execute -> launch { execute(event) } }
                    }
                    log.debug { "Processed price update for symbol '$queueName'; topic '$topicName}'; time elapsed - '${System.currentTimeMillis() - startTime}'" }
                }
                container.start()

                // Closing connection when application shutting down
                Runtime.getRuntime().addShutdownHook(Thread {
                    if (container.isRunning) {
                        container.stop()
                    }
                })
            }
        }
    }

    /**
     * Adds a new observer to the list of observers. Observers will be notified
     * of events when they occur.
     *
     * @param observer A lambda function to be called to proceed event.
     */
    fun addObserver(observer: (T) -> Unit) {
        observers.add(observer)
    }

    /**
     * Removes an existing observer from the list of observers. The removed observer
     * will no longer be notified of events.
     *
     * @param observer A lambda function representing the observer to be removed.
     */
    fun removeObserver(observer: (T) -> Unit) {
        observers.remove(observer)
    }
}
