package space.dawdawich.service.factory

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.MessageListenerContainer
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.stereotype.Service
import java.util.*

@Service
class EventListenerFactoryService(private val connectionFactory: ConnectionFactory) {

    fun <T> getPriceListener(topicName: String, queueName: String, typeRef: TypeReference<T>, function: (T) -> Unit): MessageListenerContainer {
        connectionFactory.createConnection().use {
            it.createChannel(false).use { channel ->
                val queue = channel.queueDeclare(queueName, true, false, true, emptyMap()).queue
                channel.queueBind(queue, topicName, queueName)

                val container = SimpleMessageListenerContainer(connectionFactory)
                container.acknowledgeMode = AcknowledgeMode.NONE
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

    fun <T> getPriceListenerWithRoutingKey(topicName: String, queueName: String, typeRef: TypeReference<T>, function: (String, T) -> Unit): MessageListenerContainer {
        connectionFactory.createConnection().use {
            it.createChannel(false).use { channel ->
                val queue = channel.queueDeclare(UUID.randomUUID().toString(), true, false, true, emptyMap()).queue
                channel.queueBind(queue, topicName, queueName)

                val container = SimpleMessageListenerContainer(connectionFactory)
                container.setQueueNames(queue)
                container.acknowledgeMode = AcknowledgeMode.NONE
                val mapper = jacksonObjectMapper()
                container.setMessageListener { message ->
                    val event = mapper.readValue(message.body, typeRef)
                    function(message.messageProperties.receivedRoutingKey, event)
                }
                return container
            }
        }
    }
}
