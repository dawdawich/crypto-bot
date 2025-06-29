package space.dawdawich.service

import com.fasterxml.jackson.core.type.TypeReference
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.stereotype.Service
import space.dawdawich.constants.BYBIT_TICKER_TOPIC

@Service
class NewListingService(private val connectionFactory: ConnectionFactory) {

    fun addPositionChecker(symbol: String) {
        val priceChangeListener = EventListener(connectionFactory, BYBIT_TICKER_TOPIC, symbol, object : TypeReference<Double>() {})



        priceChangeListener.addObserver { newPrice ->

        }
    }
}
