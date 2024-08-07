package space.dawdawich.services

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer

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
                    log.info { "Reacived event: $event for $topicName/$queueName" }
                    runBlocking {
                        observers.forEach { execute -> launch { execute(event) } }
                    }
                    log.debug { "Processed price update for symbol '$queueName'; topic '$topicName}'; time elapsed - '${System.currentTimeMillis() - startTime}'" }
                }
                container.start()

                Runtime.getRuntime().addShutdownHook(Thread {
                    if (container.isRunning) {
                        container.stop()
                    }
                })
            }
        }
    }

    fun addObserver(observer: (T) -> Unit) {
        observers.add(observer)
    }

    fun removeObserver(observer: (T) -> Unit) {
        observers.remove(observer)
    }
}
