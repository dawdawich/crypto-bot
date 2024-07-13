package space.dawdawich.socket

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.websocket.*
import jakarta.websocket.server.ServerEndpoint
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import space.dawdawich.configuration.WebSocketConfigurator
import space.dawdawich.constants.REQUEST_ANALYZER_RUNTIME_DATA
import space.dawdawich.model.strategy.AnalyzerRuntimeInfoModel

/**
 * The `AnalyzerEndpoint` class represents a WebSocket endpoint for analyzing data. It is responsible for handling incoming messages and sending responses.
 */
@Service
@ServerEndpoint(value = "/ws/analyzer", configurator = WebSocketConfigurator::class)
class AnalyzerEndpoint(private val rabbitTemplate: RabbitTemplate, private val mapper: ObjectMapper) {
    private val logger = KotlinLogging.logger {}

    @OnOpen
    fun onOpen(session: Session?, config: EndpointConfig?) {
        // do nothing
    }

    @OnMessage
    fun onMessage(session: Session, message: String) {
        Json.parseToJsonElement(message).jsonObject["id"]?.let { record ->
            rabbitTemplate.convertSendAndReceive(REQUEST_ANALYZER_RUNTIME_DATA, record.jsonPrimitive.content)?.let { checkedInfo ->
                val runtimeInfoJson = mapper.writeValueAsString(checkedInfo)
                session.asyncRemote.sendText(runtimeInfoJson)
            }
        }
    }

    @OnClose
    fun onClose(session: Session?, closeReason: CloseReason?) {
        // do nothing
    }

    @OnError
    fun onError(session: Session, throwable: Throwable) {
        logger.error(throwable) { "Attempt error on web socket connection." }
    }
}
