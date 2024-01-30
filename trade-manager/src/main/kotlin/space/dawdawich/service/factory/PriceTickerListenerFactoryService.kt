package space.dawdawich.service.factory

import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.support.TopicPartitionOffset
import org.springframework.stereotype.Service
import space.dawdawich.constants.BYBIT_TEST_TICKER_TOPIC
import space.dawdawich.constants.BYBIT_TICKER_TOPIC
import space.dawdawich.repositories.SymbolRepository

@Service
class PriceTickerListenerFactoryService(
    private val listenerContainerFactory: ConcurrentKafkaListenerContainerFactory<String, String>,
    symbolRepository: SymbolRepository
) {
    private val symbolsWithPartition =
        mutableMapOf(*symbolRepository.findAll().map { it.symbol to it.partition }.toTypedArray())

    fun getPriceListener(symbol: String, isTest: Boolean): ConcurrentMessageListenerContainer<String, String> =
        listenerContainerFactory.createContainer(
            TopicPartitionOffset(
                if (isTest) BYBIT_TEST_TICKER_TOPIC else BYBIT_TICKER_TOPIC,
                symbolsWithPartition[symbol] ?: throw Exception("Partition for symbol '$symbol' did not exist"),
                TopicPartitionOffset.SeekPosition.END
            )
        )
}
