package space.dawdawich.service

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import space.dawdawich.client.ByBitPriceChangeCaptureClient
import space.dawdawich.repositories.mongo.PriceTickRepository
import space.dawdawich.repositories.mongo.SymbolRepository
import java.util.concurrent.TimeUnit

@Service
class PriceTickerService(
    private val client: ByBitPriceChangeCaptureClient,
    private val symbolRepository: SymbolRepository,
    private val priceTickRepository: PriceTickRepository
) {

    private val log = KotlinLogging.logger {}

    @PostConstruct
    fun postInit() = symbolRepository.findAll()
        .filter { symbol -> !client.symbols.contains(symbol.symbol) }
        .forEach { symbol ->
            client.addSubscription(symbol.symbol)
        }
        .let { log.info { "Complete initialization" } }

    @Scheduled(fixedDelay = 3, timeUnit = TimeUnit.SECONDS)
    fun checkSocketConnection() {
        if (client.isClosed) {
            client.reconnect()
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    fun clearOldData() = priceTickRepository.deleteByTimeIsLessThan(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14))
}
