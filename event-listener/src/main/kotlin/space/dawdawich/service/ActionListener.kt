package space.dawdawich.service

import jakarta.annotation.PostConstruct
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewPartitions
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.TopicBuilder
import org.springframework.stereotype.Service
import space.dawdawich.client.BybitTickerWebSocketClient
import space.dawdawich.constants.SYMBOL_REINITIALIZE_TOPIC
import space.dawdawich.constants.TICKER_TOPIC
import space.dawdawich.repositories.SymbolRepository

@Service
class ActionListener(
    private val client: BybitTickerWebSocketClient,
    private val admin: AdminClient,
    private val symbolRepository: SymbolRepository
) {

    @PostConstruct
    fun postInit() {
        initializeTickerTopics()
        client.connect()
    }

    @KafkaListener(topics = [SYMBOL_REINITIALIZE_TOPIC])
    fun computeSymbolAction() {
        val symbolsCount = symbolRepository.count()
        if (client.mapSymbolsToPartition.size < symbolsCount) {
            admin.createPartitions(mapOf(TICKER_TOPIC to NewPartitions.increaseTo(symbolRepository.count().toInt())))
            symbolRepository.findAll().forEach { symbolInfo ->
                if (!client.mapSymbolsToPartition.containsKey(symbolInfo.symbol)) {
                    client.mapSymbolsToPartition += symbolInfo.symbol to symbolInfo.partition
                    client.addSubscription(symbolInfo.symbol)
                }
            }
        }
    }

    private fun initializeTickerTopics() {
        if (!admin.listTopics().names().get().contains(TICKER_TOPIC)) {
            admin.createTopics(
                listOf(
                    TopicBuilder.name(TICKER_TOPIC).partitions(symbolRepository.count().toInt())
                        .build()
                )
            ).all().get()
        }
        client.mapSymbolsToPartition.putAll(symbolRepository.findAll().map { it.symbol to it.partition }.toTypedArray())
    }
}
