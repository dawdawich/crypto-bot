package space.dawdawich.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import space.dawdawich.client.ByBitPriceChangeCaptureClient
import space.dawdawich.constants.BYBIT_WEB_SOCKET_URL
import space.dawdawich.repositories.mongo.PriceTickRepository

@Configuration
class ClientsConfiguration {

    @Bean
    fun byBitPriceChangeCaptureClient(priceTickRepository: PriceTickRepository) =
        ByBitPriceChangeCaptureClient(priceTickRepository, BYBIT_WEB_SOCKET_URL).apply { connectBlocking() }
}
