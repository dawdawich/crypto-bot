package space.dawdawich.service

import mu.KotlinLogging
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import space.dawdawich.constants.BACK_TEST_SERVICE
import space.dawdawich.constants.PREDEFINED_BACK_TEST_SERVICE
import space.dawdawich.exception.UnsupportedSymbolException
import space.dawdawich.model.BackTestConfiguration
import space.dawdawich.model.BacktestMessage
import space.dawdawich.model.BackTestResult
import space.dawdawich.model.GeneralBacktestMessage
import space.dawdawich.repositories.mongo.BackTestResultRepository
import space.dawdawich.repositories.mongo.RequestStatusRepository
import space.dawdawich.repositories.mongo.SymbolRepository
import space.dawdawich.repositories.mongo.entity.BackTestResultDocument
import space.dawdawich.repositories.mongo.entity.RequestStatus
import space.dawdawich.repositories.mongo.entity.SymbolDocument
import java.util.concurrent.TimeUnit

@Service
class BackTestMassageHandler(
    private val backTestService: BackTestService,
    private val symbolRepository: SymbolRepository,
    private val backTestResultRepository: BackTestResultRepository,
    private val requestStatusRepository: RequestStatusRepository
) {

    val log = KotlinLogging.logger {}

    val detailedSymbol: MutableMap<String, SymbolDocument> = mutableMapOf()

    @RabbitListener(queues = [BACK_TEST_SERVICE])
    fun startBackTest(request: BacktestMessage) {
        try {
            val configs = with(request) {
                return@with symbols.map { symbol ->
                    getSymbolData(symbol)
                }.map { symbolData ->
                    BackTestConfiguration(
                        symbolData,
                        startCapital,
                        if (multiplier <= symbolData.maxLeverage) multiplier else symbolData.maxLeverage,
                        diapason,
                        gridSize,
                        takeProfit,
                        stopLoss
                    )
                }
            }

            val backTestBulkResultDocs = backTestService.processConfigs(configs, request.startTime)
                .map { res -> resultToDocument(res, request.requestId) }

            backTestResultRepository.insert(backTestBulkResultDocs)
            requestStatusRepository.updateRequestStatus(request.requestId, RequestStatus.SUCCESS)
        } catch (e: Exception) {
            log.error(e) { "Error processing backtest" }
            requestStatusRepository.updateRequestStatus(request.requestId, RequestStatus.FAILED)
        }
    }

    @RabbitListener(queues = [PREDEFINED_BACK_TEST_SERVICE])
    fun startPredefinedBacktest(request: GeneralBacktestMessage) {
        val processStart = System.currentTimeMillis()
        log.info { "Starting predefined backtest; request id: ${request.requestId}; Start time: $processStart" }

        val symbolDocuments = symbolRepository.findAll()
        val startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)

        try {
            for (symbolDocument in symbolDocuments) {
                val configs: MutableList<BackTestConfiguration> = mutableListOf()
                for (leverage in 1..symbolDocument.maxLeverage.toInt()) {
                    for (diapason in 1..10) {
                        for (gridSize in 2..180) {
                            for (takeProfit in 1..30) {
                                for (stopLoss in 1..30) {
                                    configs += BackTestConfiguration(
                                        symbolDocument,
                                        request.startCapital,
                                        leverage.toDouble(),
                                        diapason,
                                        gridSize,
                                        takeProfit,
                                        stopLoss
                                    )
                                }
                            }
                        }
                    }
                }
                val backTestBulkResultDocs = backTestService.processConfigs(configs, startTime)
                    .map { res -> resultToDocument(res, request.requestId) }

                backTestResultRepository.insert(backTestBulkResultDocs)
            }

            requestStatusRepository.updateRequestStatus(request.requestId, RequestStatus.SUCCESS)
        } catch (e: Exception) {
            log.error(e) { "Error processing backtest" }
            requestStatusRepository.updateRequestStatus(request.requestId, RequestStatus.FAILED)
        }
        log.info { "Finish predefined backtest; request id: ${request.requestId}; Time spent: ${System.currentTimeMillis() - processStart}ms" }

    }

    private fun resultToDocument(backTestResult: BackTestResult, requestId: String): BackTestResultDocument
    = BackTestResultDocument(
        backTestResult.runConfiguration.id,
        requestId,
        backTestResult.runConfiguration.symbol.symbol,
        backTestResult.runConfiguration.startCapital,
        backTestResult.runConfiguration.multiplier,
        backTestResult.runConfiguration.diapason,
        backTestResult.runConfiguration.gridSize,
        backTestResult.runConfiguration.takeProfit,
        backTestResult.runConfiguration.stopLoss,
        backTestResult.startTime,
        backTestResult.endTime,
        backTestResult.result
    )

    private fun getSymbolData(requestedSymbol: String) = detailedSymbol.computeIfAbsent(requestedSymbol) { symbol ->
        symbolRepository.findById(symbol).orElseThrow { UnsupportedSymbolException(symbol) }
    }
}
