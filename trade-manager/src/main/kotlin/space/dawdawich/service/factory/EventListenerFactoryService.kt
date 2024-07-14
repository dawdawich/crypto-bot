package space.dawdawich.service.factory

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.MessageListenerContainer
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.stereotype.Service

@Service
class EventListenerFactoryService(private val connectionFactory: ConnectionFactory) {

    fun <T> getPriceListener(topicName: String, queueName: String, typeRef: TypeReference<T>, function: (T) -> Unit): MessageListenerContainer {
        connectionFactory.createConnection().use {
            it.createChannel(false).use { channel ->
                val queue = channel.queueDeclare(queueName, true, false, true, emptyMap()).queue
                channel.queueBind(queue, topicName, queueName)

                val container = SimpleMessageListenerContainer(connectionFactory)
                container.setQueueNames(queue)
                val mapper = jacksonObjectMapper()
                container.setMessageListener { message ->
                    val event = mapper.readValue(message.body, typeRef)
                    function(event)
                }
                return container
            }
        }
    }
}
