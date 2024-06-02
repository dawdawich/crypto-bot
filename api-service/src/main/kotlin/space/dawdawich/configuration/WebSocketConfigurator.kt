package space.dawdawich.configuration

import jakarta.websocket.server.ServerEndpointConfig
import org.springframework.stereotype.Component
import space.dawdawich.configuration.provider.ApplicationContextProvider

/**
 * The `WebSocketConfigurator` class is a configuration class used to retrieve bean instances from the application context for WebSocket endpoints.
 * It extends the `ServerEndpointConfig.Configurator` class and is annotated with `@Component` for automatic detection and registration as a component bean.
 */
@Component
class WebSocketConfigurator : ServerEndpointConfig.Configurator() {

    override fun <T : Any?> getEndpointInstance(clazz: Class<T>): T =
        ApplicationContextProvider.getApplicationContext().getBean(clazz)
}
