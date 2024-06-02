package space.dawdawich.socket

import jakarta.websocket.*
import jakarta.websocket.server.ServerEndpoint
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.configuration.WebSocketConfigurator

/**
 * The ManagerEndpoint class represents the WebSocket endpoint for the manager.
 * It handles the WebSocket connection, messages, closing, and errors.
 *
 * @param kafkaTemplate The Kafka template used for sending messages to a Kafka topic.
 */
@Service
@ServerEndpoint(value = "/ws/manager", configurator = WebSocketConfigurator::class)
class ManagerEndpoint(private val kafkaTemplate: KafkaTemplate<String, String>) {

    @OnOpen
    fun onOpen(session: Session?, config: EndpointConfig?) {
        // do nothing
    }

    @OnMessage
    fun onMessage(session: Session, message: String) {
        // do nothing
    }

    @OnClose
    fun onClose(session: Session?, closeReason: CloseReason?) {
        // do nothing
    }

    @OnError
    fun onError(session: Session, throwable: Throwable) {
        // do nothing
    }
}
