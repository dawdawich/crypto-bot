package space.dawdawich.service

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import space.dawdawich.client.BybitKLineWebSocketClient
import space.dawdawich.client.BybitTickerWebSocketClient
import space.dawdawich.repositories.mongo.SymbolRepository
import space.dawdawich.repositories.mongo.entity.SymbolInfoDocument
import java.util.concurrent.TimeUnit

@Service
class ActionListener(
    private val byBitTickerClient: BybitTickerWebSocketClient,
    private val byBitTickerDemoClient: BybitTickerWebSocketClient,
    private val byBitKLineTickerClient: BybitKLineWebSocketClient,
    private val byBitKLineTickerDemoClient: BybitKLineWebSocketClient,
    private val symbolRepository: SymbolRepository,
) {

    private val log = KotlinLogging.logger {}

    companion object {
        private val defaultKLineIntervals = arrayOf(/*1, 3,*/ 5)
    }

    @PostConstruct
    fun postInit() = symbolRepository.findAll().let { allSymbols ->
        initializeTickerTopics(allSymbols, byBitTickerClient)
//        initializeTickerTopics(allSymbols, byBitTickerDemoClient)
        initializeKLineTopics(allSymbols, byBitKLineTickerClient)
//        initializeKLineTopics(allSymbols, byBitKLineTickerDemoClient)
        log.info { "Complete initialization" }
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
    fun checkSocketsConnection() {
        if (byBitTickerClient.isClosed) {
            byBitTickerClient.reconnect()
        }
        if (byBitTickerDemoClient.isClosed) {
            byBitTickerDemoClient.reconnect()
        }
        if (byBitKLineTickerClient.isClosed) {
            byBitKLineTickerClient.reconnect()
        }
        if (byBitKLineTickerDemoClient.isClosed) {
            byBitKLineTickerDemoClient.reconnect()
        }
    }

    private fun initializeTickerTopics(symbols: List<SymbolInfoDocument>, client: BybitTickerWebSocketClient) =
        symbols.filter { !client.symbols.contains(it.symbol) }.forEach { symbol ->
            client.addSubscription(symbol.symbol)
        }

    private fun initializeKLineTopics(symbols: List<SymbolInfoDocument>, client: BybitKLineWebSocketClient) =
        try {
            runBlocking {
                symbols.filter { !client.symbols.contains(it.symbol) }.forEach { symbol ->
                    defaultKLineIntervals.forEach { interval ->
                        client.addSubscription(symbol.symbol, interval)
                    }
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Error during initialization" }
        }
}
