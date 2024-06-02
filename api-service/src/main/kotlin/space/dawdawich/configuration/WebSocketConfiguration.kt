package space.dawdawich.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.server.standard.ServerEndpointExporter

/**
 * Configuration class for WebSocket.
 */
@Configuration
class WebSocketConfiguration {
    @Bean
    fun endpointExporter() = ServerEndpointExporter()
}
