package space.dawdawich.socket

import jakarta.websocket.*
import jakarta.websocket.server.ServerEndpoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.configuration.WebSocketConfigurator
import space.dawdawich.constants.REQUEST_ANALYZER_TOPIC
import space.dawdawich.constants.RESPONSE_ANALYZER_TOPIC
import space.dawdawich.model.analyzer.GridTableDetailInfoModel

@Service
@ServerEndpoint(value = "/ws/analyzer", configurator = WebSocketConfigurator::class)
class AnalyzerEndpoint(
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
            val analyzerId = body.jsonObject["id"]!!.jsonPrimitive.content
            connections[session]?.let { ids ->
                ids += analyzerId
            }
            kafkaTemplate.send(REQUEST_ANALYZER_TOPIC, analyzerId)
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
        topics = [RESPONSE_ANALYZER_TOPIC],
        groupId = "analyzer_info_group",
        containerFactory = "analyzerInfoDocumentKafkaListenerContainerFactory"
    )
    fun getAnalyzerInfo(analyzerInfo: GridTableDetailInfoModel) {
        connections.filter { it.value.contains(analyzerInfo.id) }.keys.forEach { session ->
            session.asyncRemote.sendText(Json.encodeToString(analyzerInfo))
        }
    }
}
