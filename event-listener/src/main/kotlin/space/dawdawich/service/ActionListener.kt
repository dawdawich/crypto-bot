package space.dawdawich.service

import jakarta.annotation.PostConstruct
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewPartitions
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.TopicBuilder
import org.springframework.stereotype.Service
import space.dawdawich.client.BybitTickerWebSocketClient
import space.dawdawich.constants.BYBIT_TEST_TICKER_TOPIC
import space.dawdawich.constants.SYMBOL_REINITIALIZE_TOPIC
import space.dawdawich.constants.BYBIT_TICKER_TOPIC
import space.dawdawich.repositories.SymbolRepository
import space.dawdawich.repositories.entity.SymbolInfoDocument

@Service
class ActionListener(
    private val byBitClient: BybitTickerWebSocketClient,
    private val byBitTestClient: BybitTickerWebSocketClient,
    private val admin: AdminClient,
    private val symbolRepository: SymbolRepository
) {

    @PostConstruct
    fun postInit() {
        val allSymbols = symbolRepository.findAll()
        val symbols = allSymbols.filter { !it.testServer }
        val testSymbols = allSymbols.filter { it.testServer }
        initializeTickerTopics(BYBIT_TICKER_TOPIC, symbols, byBitClient)
        initializeTickerTopics(BYBIT_TEST_TICKER_TOPIC, testSymbols, byBitTestClient)
    }

    @KafkaListener(topics = [SYMBOL_REINITIALIZE_TOPIC])
    fun computeSymbolAction() {
        postInit()
    }

    private fun initializeTickerTopics(topic: String, symbols: List<SymbolInfoDocument>, client: BybitTickerWebSocketClient) {
        if (symbols.isNotEmpty()) {
            if (!admin.listTopics().names().get().contains(topic)) {
                createTickerTopics(symbols.size, topic)
            } else {
                val topicDescription = admin.describeTopics(listOf(topic)).topicNameValues()[topic]?.get()
                val currentPartitionsCount = topicDescription?.partitions()?.size ?: 0
                if (currentPartitionsCount < symbols.size) {
                    admin.createPartitions(mapOf(topic to NewPartitions.increaseTo(symbols.size)))
                }
            }
            client.mapSymbolsToPartition.putAll(symbols.map { it.symbol to it.partition }.toTypedArray())
            client.connect()
        }
    }

    private fun createTickerTopics(symbolCount: Int, topic: String) {
        admin.createTopics(
            listOf(
                TopicBuilder.name(topic).partitions(symbolCount)
                    .build()
            )
        ).all().get()
    }
}
