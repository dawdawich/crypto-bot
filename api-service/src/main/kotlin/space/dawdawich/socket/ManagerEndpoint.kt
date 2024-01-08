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

    private val connections: MutableMap<Session, MutableSet<String>> = mutableMapOf()

    @OnOpen
    fun onOpen(session: Session?, config: EndpointConfig?) {
        session?.let { checkedSession ->
            connections[checkedSession] = mutableSetOf()
        }
    }

    @OnMessage
    fun onMessage(session: Session, message: String) {
        val body = Json.parseToJsonElement(message)
        if (body.jsonObject.contains("id")) {
            val managerId = body.jsonObject["id"]!!.jsonPrimitive.content
            connections[session]?.let { ids ->
                ids += managerId
            }
            kafkaTemplate.send(REQUEST_MANAGER_TOPIC, managerId)
        }
    }

    @OnClose
    fun onClose(session: Session?, closeReason: CloseReason?) {
        session.let { checkedSession ->
            connections.remove(checkedSession)
        }
    }

    @OnError
    fun onError(session: Session, throwable: Throwable) {
        logger.error(throwable) { "Attempt error on web socket connection." }
        connections.remove(session)
    }

    @KafkaListener(
        topics = [RESPONSE_MANAGER_TOPIC],
        groupId = "manager_info_group",
        containerFactory = "managerInfoDocumentKafkaListenerContainerFactory"
    )
    fun getManagerInfo(managerInfo: ManagerInfoModel) {
        connections.filter { entry -> entry.value.contains(managerInfo.id) }.keys.forEach { session ->
            session.asyncRemote.sendText(Json.encodeToString(managerInfo))
        }
    }
}
