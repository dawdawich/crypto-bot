package space.dawdawich.socket

import jakarta.websocket.*
import jakarta.websocket.server.ServerEndpoint
import mu.KotlinLogging
import org.springframework.stereotype.Service
import space.dawdawich.configuration.WebSocketConfigurator
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import space.dawdawich.service.RequestStatusService
import space.dawdawich.socket.model.RequestStatusResponse


@Service
@ServerEndpoint(value = "/ws/backtest", configurator = WebSocketConfigurator::class)
class BacktestWebSocket(
    private val requestStatusService: RequestStatusService,
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        val jsonPath = JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))!!
    }

    @OnOpen
    @SuppressWarnings("unused")
    fun onOpen(session: Session?, config: EndpointConfig?) {
        // do nothing
    }

    @OnMessage
    @SuppressWarnings("unused")
    fun onMessage(session: Session, message: String) {
        val processedMessage = jsonPath.parse(message)
        val type = processedMessage.read<MessageType>("\$.type")

        if (type == MessageType.STATUS_CHECK) {
            val requestId = processedMessage.read<String>("\$.requestId")

            requestStatusService.getRequestStatus(requestId)?.let {
                session.asyncRemote.sendObject(RequestStatusResponse(it))
            }
        }
    }

    @OnClose
    @SuppressWarnings("unused")
    fun onClose(session: Session?, closeReason: CloseReason?) {
        // do nothing
    }

    @OnError
    @SuppressWarnings("unused")
    fun onError(session: Session, throwable: Throwable) {
        logger.error(throwable) { "Attempt error on web socket connection." }
    }

    enum class MessageType {
        STATUS_CHECK
    }
}
