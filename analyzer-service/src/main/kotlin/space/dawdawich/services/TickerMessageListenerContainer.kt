package space.dawdawich.services

import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer

class TickerMessageListenerContainer(connectionFactory: ConnectionFactory) : SimpleMessageListenerContainer(connectionFactory) {

}
