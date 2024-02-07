package space.dawdawich.service

import jakarta.annotation.PostConstruct
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewPartitions
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.TopicBuilder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import space.dawdawich.client.BybitTickerWebSocketClient
import space.dawdawich.constants.BYBIT_TEST_TICKER_TOPIC
import space.dawdawich.constants.SYMBOL_REINITIALIZE_TOPIC
import space.dawdawich.constants.BYBIT_TICKER_TOPIC
import space.dawdawich.repositories.SymbolRepository
import space.dawdawich.repositories.entity.SymbolInfoDocument
import java.util.concurrent.TimeUnit

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

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
    fun checkSocketsConnection() {
        if (byBitClient.isClosed) {
            byBitClient.reconnect()
        }
        if (byBitTestClient.isClosed) {
            byBitTestClient.reconnect()
        }
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
            val existedKeys = client.mapSymbolsToPartition.keys
            val symbolsNeedToAdd = symbols.filter { !existedKeys.contains(it.symbol) }
            symbolsNeedToAdd.forEach { symbol ->
                client.addSubscription(symbol.symbol)
            }
            if (symbolsNeedToAdd.isNotEmpty()) {
                client.mapSymbolsToPartition.putAll(symbolsNeedToAdd.map { it.symbol to it.partition }.toTypedArray())
            }
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
