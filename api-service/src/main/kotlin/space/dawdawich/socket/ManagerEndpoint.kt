package space.dawdawich.socket

import jakarta.websocket.*
import jakarta.websocket.server.ServerEndpoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.configuration.WebSocketConfigurator
import space.dawdawich.constants.REQUEST_MANAGER_TOPIC
import space.dawdawich.constants.RESPONSE_MANAGER_TOPIC
import space.dawdawich.model.manager.ManagerInfoModel

@Service
@ServerEndpoint(value = "/ws/manager", configurator = WebSocketConfigurator::class)
class ManagerEndpoint(
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val logger = KotlinLogging.logger {}

    @OnOpen
    fun onOpen(session: Session?, config: EndpointConfig?) {
    }

    @OnMessage
    fun onMessage(session: Session, message: String) {
    }

    @OnClose
    fun onClose(session: Session?, closeReason: CloseReason?) {
    }

    @OnError
    fun onError(session: Session, throwable: Throwable) {
    }
}
