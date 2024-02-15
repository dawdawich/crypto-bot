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
import space.dawdawich.constants.REQUEST_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC
import space.dawdawich.model.strategy.StrategyRuntimeInfoModel
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Service
@ServerEndpoint(value = "/ws/analyzer", configurator = WebSocketConfigurator::class)
class AnalyzerEndpoint(
    private val strategyRuntimeDataReplyingTemplate: ReplyingKafkaTemplate<String, String, StrategyRuntimeInfoModel?>
) {
    private val logger = KotlinLogging.logger {}

    @OnOpen
    fun onOpen(session: Session?, config: EndpointConfig?) {

    }

    @OnMessage
    fun onMessage(session: Session, message: String) {
        Json.parseToJsonElement(message).jsonObject["id"]?.let { record ->
            val runtimeInfo = try { strategyRuntimeDataReplyingTemplate.sendAndReceive(
                ProducerRecord(
                    REQUEST_ANALYZER_STRATEGY_RUNTIME_DATA_TOPIC, record.jsonPrimitive.content
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
    }

    @OnError
    fun onError(session: Session, throwable: Throwable) {
        logger.error(throwable) { "Attempt error on web socket connection." }
    }
}
