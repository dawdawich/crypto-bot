package space.dawdawich.services

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.kafka.common.TopicPartition
import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Fields.UNDERSCORE_ID
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.CriteriaDefinition
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.where
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.support.TopicPartitionOffset
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import space.dawdawich.analyzers.GridTableAnalyzer
import space.dawdawich.constants.TICKER_TOPIC
import space.dawdawich.data.Trend
import space.dawdawich.repositories.AnalyzerPositionRepository
import space.dawdawich.repositories.GridTableAnalyzerReactiveRepository
import space.dawdawich.repositories.GridTableAnalyzerRepository
import space.dawdawich.repositories.SymbolRepository
import space.dawdawich.repositories.entity.AnalyzerPositionDocument
import space.dawdawich.repositories.entity.GridTableAnalyzerDocument
import space.dawdawich.repositories.entity.SymbolInfoDocument
import space.dawdawich.utils.plusPercent
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

@Service
open class AnalyzerService(
    private val listenerContainerFactory: ConcurrentKafkaListenerContainerFactory<String, String>,
    private val symbolRepository: SymbolRepository,
    private val gridTableAnalyzerRepository: GridTableAnalyzerRepository,
    private val mongoTemplate: MongoTemplate
) {

    private val priceListeners = mutableMapOf<Int, PriceTickerListener>()
    private val partitionMap: MutableMap<String, Int> =
        mutableMapOf(*symbolRepository.findAll().map { it.symbol to it.partition }.toTypedArray())
    private val analyzers: MutableList<GridTableAnalyzer> = mutableListOf()
    private val moneyUpdateList: CopyOnWriteArrayList<Pair<String, Double>> = CopyOnWriteArrayList()
    private val middlePriceUpdateList: CopyOnWriteArrayList<Pair<String, Double>> = CopyOnWriteArrayList()

    init {
//        data class Info(val symbol: String, val partition: Int, val oneWay: Boolean, val minPrice: Double, val minQuantity: Double, val price: Double)
//        val pairInstructions = listOf(
//            Info("BTCUSDT", 0, false, 0.1, 0.001, 44040.2),
//            Info("ETHUSDT", 1, false, 0.01, 0.01, 2273.39),
//            Info("SOLUSDT", 2, false, 0.001, 0.1, 64.05),
//            Info("ORDIUSDT", 3, true, 0.001, 0.01, 46.5),
//            Info("DOGEUSDT", 4, false, 0.00001, 1.0, 0.103),
//            Info("TIAUSDT", 5, true, 0.0001, 0.1, 9.757),
//            Info("XRPUSDT", 6, true, 0.0001, 1.0, 0.63),
//            Info("LINKUSDT", 7, true, 0.001, 0.1, 15.86),
//            Info("ETCUSDT", 8, true, 0.001, 0.1, 20.49),
//        )
//        val toInsert = mutableListOf<GridTableAnalyzerDocument>()
//        for (info in pairInstructions) {
//            for (stopLoss in 2..9) {
//                for (takeProfit in 2..9) {
//                    for (diapasonPercent in 1..3) {
//                        for (gridSize in 10..150 step 10) {
//                            for (multiplier in 15..25) {
//                                val minPrice = info.price.plusPercent(-diapasonPercent)
//                                val maxPrice = info.price.plusPercent(diapasonPercent)
//                                val step = (maxPrice - minPrice) / gridSize
//                                val qty = 10 / minPrice
//
//                                if (step < info.minPrice || qty < info.minQuantity) {
//                                    continue
//                                }
//
//                                toInsert +=
//                                    GridTableAnalyzerDocument(
//                                        UUID.randomUUID().toString(),
//                                        diapasonPercent,
//                                        gridSize,
//                                        multiplier,
//                                        stopLoss,
//                                        takeProfit,
//                                        SymbolInfoDocument(info.symbol, info.partition, info.oneWay, info.minPrice),
//                                        10.0,
//                                        false
//                                    )
//
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        gridTableAnalyzerRepository.insert(toInsert)
        gridTableAnalyzerRepository.findAll().filter { it.isActive }.map {
            GridTableAnalyzer(
                it.diapason,
                it.gridSize,
                it.money,
                it.multiplayer,
                it.positionStopLoss,
                it.positionTakeProfit,
                it.symbolInfo.symbol,
                it.symbolInfo.isOneWayMode,
                it.symbolInfo.priceMinStep,
                it.id,
                { _, _, newValue -> moneyUpdateList += it.id to newValue },
                { middlePrice -> middlePriceUpdateList += it.id to middlePrice }
            )
        }.toMutableList().forEach { addAnalyzer(it, false) }
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
    fun processMiddlePriceUpdateList() {
        if (middlePriceUpdateList.isNotEmpty()) {
            val ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, GridTableAnalyzerDocument::class.java)

            middlePriceUpdateList.removeAll(middlePriceUpdateList.toList().map {
                ops.updateOne(
                    Query.query(Criteria.where(UNDERSCORE_ID).`is`(it.first)),
                    Update().set("middlePrice", it.second)
                )
                it
            }.toSet())
            ops.execute()
        }
        if (moneyUpdateList.isNotEmpty()) {
            val ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, GridTableAnalyzerDocument::class.java)

            moneyUpdateList.removeAll(moneyUpdateList.map {
                ops.updateOne(
                    Query.query(Criteria.where(UNDERSCORE_ID).`is`(it.first)),
                    Update().set("money", it.second)
                )
                it
            }.toSet())
            ops.execute()
        }
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
                    listenerContainerFactory.createContainer(TopicPartitionOffset(TICKER_TOPIC, partition, TopicPartitionOffset.SeekPosition.END))
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
                            SymbolInfoDocument(symbol, partition, isOneWayMode, priceMinStep),
                            money,
                            true
                        )
                    )
                }
            }
        } ?: run { throw Exception("Could not find partition for provided symbol: '${analyzer.symbol}'") }
    }
}
