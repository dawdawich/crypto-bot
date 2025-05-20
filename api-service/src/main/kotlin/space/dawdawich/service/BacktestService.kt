package space.dawdawich.service

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.BACK_TEST_SERVICE
import space.dawdawich.constants.PREDEFINED_BACK_TEST_SERVICE
import space.dawdawich.controller.model.backtest.BacktestRequest
import space.dawdawich.controller.model.backtest.BacktestRequestResultsResponse
import space.dawdawich.controller.model.backtest.BacktestResultDetail
import space.dawdawich.controller.model.backtest.PredefinedBacktestRequest
import space.dawdawich.model.BacktestMessage
import space.dawdawich.model.GeneralBacktestMessage
import space.dawdawich.repositories.mongo.BackTestResultRepository

@Service
class BacktestService(
    private val requestStatusService: RequestStatusService,
    private val backTestResultRepository: BackTestResultRepository,
    private val rabbitTemplate: RabbitTemplate,
) {

    fun createBacktest(backtestRequest: BacktestRequest, accountId: String, requestId: String) {
        requestStatusService.createRequestStatus(requestId, accountId)

        with(backtestRequest) {
            rabbitTemplate.convertAndSend(
                BACK_TEST_SERVICE,
                BacktestMessage(
                    requestId,
                    symbols,
                    startCapital,
                    leverage,
                    diapason,
                    gridSize,
                    takeProfit,
                    stopLoss,
                    startTime
                )
            )
        }
    }

    fun createPredefinedBacktest(backtestRequest: PredefinedBacktestRequest, accountId: String, requestId: String) {
        requestStatusService.createRequestStatus(requestId, accountId)

        with(backtestRequest) {
            rabbitTemplate.convertAndSend(
                PREDEFINED_BACK_TEST_SERVICE,
                GeneralBacktestMessage(
                    requestId,
                    startCapital,
                )
            )
        }
    }

    fun getBackTestResults(requestId: String): BacktestRequestResultsResponse? {
        val results =
            backTestResultRepository.getByRequestId(requestId).sortedByDescending { it.finalCapital }.let {
                if (it.size > 50) {
                    return@let it.subList(0, 50)
                }
                it
            }

        if (results.isEmpty()) {
            return null
        }

        return results[0].let { config ->
            BacktestRequestResultsResponse(
                config.startCapital,
                results.map { res ->
                    BacktestResultDetail(
                        config.diapason,
                        config.gridSize,
                        config.takeProfit,
                        config.stopLoss,
                        res.symbol,
                        res.multiplier,
                        res.finalCapital,
                        res.startTime,
                        res.endTime,
                    )
                }
            )
        }
    }
}
