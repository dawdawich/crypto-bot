package space.dawdawich.service

import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.support.TopicPartitionOffset
import org.springframework.stereotype.Service
import space.dawdawich.constants.TICKER_TOPIC
import space.dawdawich.repositories.SymbolRepository

@Service
class PriceTickerListenerFactoryService(private val listenerContainer: ConcurrentKafkaListenerContainerFactory<String, String>,
                                        symbolRepository: SymbolRepository) {
    private val symbolsWithPartition = mutableMapOf(*symbolRepository.findAll().map { it.symbol to it.partition }.toTypedArray())

    fun getPriceListener(symbol: String): ConcurrentMessageListenerContainer<String, String> =
        listenerContainer.createContainer(TopicPartitionOffset(TICKER_TOPIC, symbolsWithPartition[symbol] ?: throw Exception("Partition for symbol '$symbol' did not exist"), TopicPartitionOffset.SeekPosition.END))
}
