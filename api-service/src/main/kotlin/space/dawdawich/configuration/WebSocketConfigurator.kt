package space.dawdawich.configuration

import jakarta.websocket.server.ServerEndpointConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import space.dawdawich.configuration.provider.ApplicationContextProvider

@Component
class WebSocketConfigurator : ServerEndpointConfig.Configurator() {

    override fun <T : Any?> getEndpointInstance(clazz: Class<T>): T =
        ApplicationContextProvider.getApplicationContext().getBean(clazz)
}
