package space.dawdawich.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import space.dawdawich.client.BybitTickerWebSocketClient
import space.dawdawich.constants.BYBIT_TEST_WEB_SOCKET_URL
import space.dawdawich.constants.BYBIT_WEB_SOCKET_URL
import space.dawdawich.service.KafkaManager

@Configuration
open class ClientsConfiguration {

    @Bean
    open fun byBitClient(kafkaManager: KafkaManager): BybitTickerWebSocketClient = BybitTickerWebSocketClient(kafkaManager, BYBIT_WEB_SOCKET_URL, false).apply { connectBlocking() }

    @Bean
    open fun byBitTestClient(kafkaManager: KafkaManager): BybitTickerWebSocketClient = BybitTickerWebSocketClient(kafkaManager, BYBIT_TEST_WEB_SOCKET_URL, true).apply { connectBlocking() }
}
