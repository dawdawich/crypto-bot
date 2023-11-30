package space.dawdawich.services

import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.support.TopicPartitionOffset
import org.springframework.stereotype.Service
import space.dawdawich.analyzers.GridTableAnalyzer
import space.dawdawich.constants.TICKER_TOPIC
import space.dawdawich.data.Trend
import space.dawdawich.repositories.AnalyzerPositionRepository
import space.dawdawich.repositories.GridTableAnalyzerRepository
import space.dawdawich.repositories.SymbolRepository
import space.dawdawich.repositories.entity.AnalyzerPositionDocument
import space.dawdawich.repositories.entity.GridTableAnalyzerDocument
import space.dawdawich.repositories.entity.SymbolInfoDocument

@Service
open class AnalyzerService(
    private val listenerContainerFactory: ConcurrentKafkaListenerContainerFactory<String, String>,
    private val symbolRepository: SymbolRepository,
    private val gridTableAnalyzerRepository: GridTableAnalyzerRepository,
    private val analyzerPositionRepository: AnalyzerPositionRepository
) {

    private val priceListeners = mutableMapOf<Int, PriceTickerListener>()
    private val partitionMap: MutableMap<String, Int> =
        mutableMapOf(*symbolRepository.findAll().map { it.symbol to it.partition }.toTypedArray())
    private val analyzers: MutableList<GridTableAnalyzer> = mutableListOf()

    init {
        gridTableAnalyzerRepository.findAll().map {
            GridTableAnalyzer(
                it.diapason,
                it.gridSize,
                it.money,
                it.multiplayer,
                it.positionStopLoss,
                it.positionTakeProfit,
                it.symbolInfo.symbol,
                it.id,
                { _, _, newValue -> gridTableAnalyzerRepository.updateMoney(it.id, newValue) },
                { _, oldValue, newValue ->
                    oldValue?.let { position ->
                        analyzerPositionRepository.markPositionAsComplete(position.id, position.closePrice, position.entryPrice, position.size)
                    }
                    newValue?.let {position ->
                        analyzerPositionRepository.insert(AnalyzerPositionDocument(
                            position.id,
                            it.id,
                            position.entryPrice,
                            position.size,
                            position.trend == Trend.SHORT,
                            null,
                            null
                        ))
                    }
                }
            )
        }.toMutableList().forEach { addAnalyzer(it, false) }
    }

    fun insertListener(partition: Int) {
        priceListeners[partition]?.stopContainer()
        priceListeners.remove(partition)
        priceListeners[partition] = PriceTickerListener(
            listenerContainerFactory.createContainer(TopicPartitionOffset(TICKER_TOPIC, partition))
        )
    }

    fun getListener(partition: Int): PriceTickerListener? {
        return priceListeners[partition]
    }

    fun addAnalyzer(analyzer: GridTableAnalyzer, isNewAnalyzer: Boolean = true) {
        partitionMap[analyzer.symbol]?.let { partition ->
            priceListeners.getOrPut(partition) {
                PriceTickerListener(
                    listenerContainerFactory.createContainer(TopicPartitionOffset(TICKER_TOPIC, partition))
                )
            }.addObserver { previousPrice, currentPrice ->
                analyzer.acceptPriceChange(previousPrice, currentPrice)
            }
            analyzers += analyzer

            if (isNewAnalyzer) {
                analyzer.apply {
                    gridTableAnalyzerRepository.insert(
                        GridTableAnalyzerDocument(
                            id,
                            diapason,
                            gridSize,
                            multiplier,
                            stopLoss,
                            takeProfit,
                            SymbolInfoDocument(symbol, partition),
                            money,
                            true
                        )
                    )
                }
            }
        } ?: run { throw Exception("Could not find partition for provided symbol: '${analyzer.symbol}'") }
    }
}
