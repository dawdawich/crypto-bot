package space.dawdawich.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import space.dawdawich.client.AbstractWebSocketClient
import space.dawdawich.client.BybitKLineWebSocketClient
import space.dawdawich.client.BybitTickerWebSocketClient
import space.dawdawich.constants.BYBIT_TEST_WEB_SOCKET_URL
import space.dawdawich.constants.BYBIT_WEB_SOCKET_URL
import space.dawdawich.service.RabbitManager

@Configuration
class ClientsConfiguration {

    @Bean
    fun byBitTickerClient(rabbitManager: RabbitManager): BybitTickerWebSocketClient = BybitTickerWebSocketClient(rabbitManager, BYBIT_WEB_SOCKET_URL, false).apply { connectBlocking() }

    @Bean
    fun byBitTickerDemoClient(rabbitManager: RabbitManager): BybitTickerWebSocketClient = BybitTickerWebSocketClient(rabbitManager, BYBIT_TEST_WEB_SOCKET_URL, true).apply { connectBlocking() }

    @Bean
    fun byBitKLineTickerClient(rabbitManager: RabbitManager): BybitKLineWebSocketClient = BybitKLineWebSocketClient(rabbitManager, BYBIT_WEB_SOCKET_URL, false).apply { connectBlocking() }

    @Bean
    fun byBitKLineTickerDemoClient(rabbitManager: RabbitManager): BybitKLineWebSocketClient = BybitKLineWebSocketClient(rabbitManager, BYBIT_TEST_WEB_SOCKET_URL, true).apply { connectBlocking() }
}
