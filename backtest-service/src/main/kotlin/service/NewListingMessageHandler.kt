package space.dawdawich.service

import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import space.dawdawich.constants.NEW_LISTING_SERVICE
import space.dawdawich.repositories.mongo.SymbolRepository

@Service
class NewListingMessageHandler(private val symbolRepository: SymbolRepository) {

    @RabbitListener(queues = [NEW_LISTING_SERVICE])
    fun getNewSymbolListingMessage(symbol: String) {

    }
}
