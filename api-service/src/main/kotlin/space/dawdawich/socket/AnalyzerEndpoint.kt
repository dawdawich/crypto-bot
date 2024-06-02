package space.dawdawich.socket

import jakarta.websocket.*
import jakarta.websocket.server.ServerEndpoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate
import org.springframework.stereotype.Service
import space.dawdawich.configuration.WebSocketConfigurator
import space.dawdawich.constants.REQUEST_ANALYZER_RUNTIME_DATA
import space.dawdawich.model.strategy.AnalyzerRuntimeInfoModel
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * The `AnalyzerEndpoint` class represents a WebSocket endpoint for analyzing data. It is responsible for handling incoming messages and sending responses.
 *
 * @property strategyRuntimeDataReplyingTemplate The Kafka template for sending and receiving runtime information for analyzers.
 */
@Service
@ServerEndpoint(value = "/ws/analyzer", configurator = WebSocketConfigurator::class)
class AnalyzerEndpoint(
    private val strategyRuntimeDataReplyingTemplate: ReplyingKafkaTemplate<String, String, AnalyzerRuntimeInfoModel?>
) {
    private val logger = KotlinLogging.logger {}

    @OnOpen
    fun onOpen(session: Session?, config: EndpointConfig?) {
        // do nothing
    }

    @OnMessage
    fun onMessage(session: Session, message: String) {
        Json.parseToJsonElement(message).jsonObject["id"]?.let { record ->
            val runtimeInfo = try { strategyRuntimeDataReplyingTemplate.sendAndReceive(
                ProducerRecord(
                    REQUEST_ANALYZER_RUNTIME_DATA, record.jsonPrimitive.content
                )
            ).get(5, TimeUnit.SECONDS).value() } catch (ex: TimeoutException) { null }
            runtimeInfo?.let { checkedInfo ->
                val runtimeInfoJson = Json.encodeToString(checkedInfo)
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
