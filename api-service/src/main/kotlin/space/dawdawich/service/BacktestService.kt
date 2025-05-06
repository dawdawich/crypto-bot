package space.dawdawich.service

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import space.dawdawich.constants.BACK_TEST_SERVICE
import space.dawdawich.constants.BACK_TEST_SERVICE_BULK
import space.dawdawich.controller.model.backtest.BacktestBulkRequest
import space.dawdawich.controller.model.backtest.BacktestRequest
import space.dawdawich.model.BacktestBulkMessage
import space.dawdawich.model.BacktestMessage

@Service
class BacktestService(
    private val requestStatusService: RequestStatusService,
    private val rabbitTemplate: RabbitTemplate,
) {

    fun createBacktest(backtestRequest: BacktestRequest, accountId: String, requestId: String) {
        requestStatusService.createRequestStatus(requestId, accountId)

        with(backtestRequest) {
            rabbitTemplate.convertAndSend(
                BACK_TEST_SERVICE,
                BacktestMessage(
                    requestId,
                    symbol,
                    startCapital,
                    multiplier,
                    diapason,
                    gridSize,
                    takeProfit,
                    stopLoss,
                    startTime
                )
            )
        }
    }

    fun createBacktestBulk(backtestRequest: BacktestBulkRequest, accountId: String, requestId: String) {
        requestStatusService.createRequestStatus(requestId, accountId)

        with(backtestRequest) {
            rabbitTemplate.convertAndSend(
                BACK_TEST_SERVICE_BULK,
                BacktestBulkMessage(
                    requestId,
                    symbol,
                    startCapital,
                    multiplier,
                    diapason,
                    gridSize,
                    takeProfit,
                    stopLoss,
                    startTime
                )
            )
        }
    }
}
