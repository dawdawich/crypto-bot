package space.dawdawich.service

import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import space.dawdawich.constants.BACK_TEST_SERVICE
import space.dawdawich.constants.BACK_TEST_SERVICE_BULK
import space.dawdawich.exception.UnsupportedSymbolException
import space.dawdawich.model.BacktestBulkMessage
import space.dawdawich.model.BackTestConfiguration
import space.dawdawich.model.BacktestMessage
import space.dawdawich.model.BackTestResult
import space.dawdawich.repositories.mongo.BackTestResultRepository
import space.dawdawich.repositories.mongo.RequestStatusRepository
import space.dawdawich.repositories.mongo.SymbolRepository
import space.dawdawich.repositories.mongo.entity.BackTestResultDocument
import space.dawdawich.repositories.mongo.entity.RequestStatus
import space.dawdawich.repositories.mongo.entity.SymbolDocument

@Service
class BackTestMassageHandler(
    private val backTestService: BackTestService,
    private val symbolRepository: SymbolRepository,
    private val backTestResultRepository: BackTestResultRepository,
    private val requestStatusRepository: RequestStatusRepository
) {

    val detailedSymbol: MutableMap<String, SymbolDocument> = mutableMapOf()

    @RabbitListener(queues = [BACK_TEST_SERVICE])
    fun startBackTest(request: BacktestMessage) {
        try {
            val symbolData = getSymbolData(request.symbol)

            val backTestResult: BackTestResult = with(request) {
                backTestService.processConfig(
                    BackTestConfiguration(
                        symbolData,
                        startCapital,
                        multiplier,
                        diapason,
                        gridSize,
                        takeProfit,
                        stopLoss,
                    ),
                    startTime
                )
            }

            backTestResultRepository.insert(resultToDocument(backTestResult, request.requestId))
            requestStatusRepository.updateRequestStatus(request.requestId, RequestStatus.SUCCESS)
        } catch (e: Exception) {
            requestStatusRepository.updateRequestStatus(request.requestId, RequestStatus.FAILED)
        }
    }

    @RabbitListener(queues = [BACK_TEST_SERVICE_BULK])
    fun startBackTestBulk(bulkRequest: BacktestBulkMessage) {
        try {
            val configurationList: MutableList<BackTestConfiguration> = mutableListOf()

            for (symbolData in bulkRequest.symbol.map { symbol -> getSymbolData(symbol) }) {
                for (multiplier in bulkRequest.multiplier.toRange()) {
                    for (diapason in bulkRequest.diapason.toRange()) {
                        for (gridSize in bulkRequest.gridSize.toRange()) {
                            for (takeProfit in bulkRequest.takeProfit.toRange()) {
                                for (stopLoss in bulkRequest.stopLoss.toRange()) {
                                    configurationList += BackTestConfiguration(
                                        symbolData,
                                        bulkRequest.startCapital,
                                        multiplier,
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
            }

            val backTestBulkResultDocs = backTestService.processConfigs(configurationList, bulkRequest.startTime)
                .map { res -> resultToDocument(res, bulkRequest.requestId) }

            backTestResultRepository.insert(backTestBulkResultDocs)
            requestStatusRepository.updateRequestStatus(bulkRequest.requestId, RequestStatus.SUCCESS)
        } catch (e: Exception) {
            requestStatusRepository.updateRequestStatus(bulkRequest.requestId, RequestStatus.FAILED)
        }
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

    private fun Pair<Int, Int>.toRange() = IntRange(this.first, this.second)

    private fun getSymbolData(requestedSymbol: String) = detailedSymbol.computeIfAbsent(requestedSymbol) { symbol ->
        symbolRepository.findById(symbol).orElseThrow { UnsupportedSymbolException(symbol) }
    }
}
