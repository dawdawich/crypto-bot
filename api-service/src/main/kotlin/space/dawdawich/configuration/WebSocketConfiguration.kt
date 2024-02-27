package space.dawdawich.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.socket.server.standard.ServerEndpointExporter
import org.springframework.web.socket.server.standard.ServerEndpointRegistration
import space.dawdawich.socket.AnalyzerEndpoint

@Configuration
class WebSocketConfiguration {
    @Bean
    fun endpointExporter() = ServerEndpointExporter()
}
