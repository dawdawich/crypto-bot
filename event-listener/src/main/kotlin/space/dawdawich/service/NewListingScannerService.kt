package space.dawdawich.service

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import space.dawdawich.client.ByBitPriceChangeCaptureClient
import space.dawdawich.integration.client.bybit.ByBitPublicHttpClient
import space.dawdawich.integration.client.telegram.TelegramApiClient
import space.dawdawich.repositories.mongo.SymbolRepository
import space.dawdawich.repositories.mongo.entity.SymbolDocument
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@Service
class NewListingScannerService(
    private val publicHttpClient: ByBitPublicHttpClient,
    private val symbolRepository: SymbolRepository,
    private val client: ByBitPriceChangeCaptureClient,
    private val telegramBot: TelegramApiClient,
    @Value("\${app.api-token}") private val apiToken: String
    ) {

    val log = KotlinLogging.logger {}

    @PostConstruct
    fun postInit() {
        runBlocking { telegramBot.sendMessage(apiToken, -1002713239108, "Startup complete") }
    }

    @OptIn(ExperimentalTime::class)
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    fun processNewListing() {
        try {
            val savedSymbols: MutableList<SymbolDocument> = symbolRepository.findAll()

            val fetchedSymbols = runBlocking { publicHttpClient.getPairInstructionsWithCursor() }.toMutableList()

            val symbolsToAdd =
                fetchedSymbols.filter { fetched -> savedSymbols.none { saved -> saved.symbol == fetched.name } }

            symbolsToAdd.forEach { symbol ->
                client.addSubscription(symbol.name)
            }
            symbolRepository.saveAll(symbolsToAdd.map {
                SymbolDocument(
                    it.name,
                    it.minPrice,
                    it.maxPrice,
                    it.minOrderQty,
                    it.maxOrderQty,
                    it.maxLeverage,
                    it.leverageStep,
                    it.qtyStep,
                    it.launchTime
                )
            })

            symbolsToAdd
                .filter { fetched ->
                    fetched.launchTime > Clock.System.now().minus(10.minutes).toEpochMilliseconds()
                }.forEach { saved ->
                    runBlocking { telegramBot.sendMessage(apiToken, -1002713239108, saved.name) }
                }

            log.info { "Successfully processed  ${symbolsToAdd.size} new symbols" }
        } catch (e: Exception) {
            log.error(e) { "Error during processing symbol list" }
        }
    }
}
