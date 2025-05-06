package space.dawdawich.controller

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import space.dawdawich.controller.model.backtest.BacktestBulkRequest
import space.dawdawich.controller.model.backtest.BacktestRequest
import space.dawdawich.controller.model.backtest.RequestIdResponse
import space.dawdawich.service.BacktestService
import java.util.*

@RestController
@RequestMapping("/backtest")
class BacktestController(
    private val backtestService: BacktestService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createBacktest(@RequestBody request: BacktestRequest, authentication: Authentication): RequestIdResponse {
        val requestId = UUID.randomUUID().toString()
        backtestService.createBacktest(request, authentication.name, requestId)

        return RequestIdResponse(requestId)
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    fun createBacktestBulk(@RequestBody request: BacktestBulkRequest, authentication: Authentication): RequestIdResponse {
        val requestId = UUID.randomUUID().toString()
        backtestService.createBacktestBulk(request, authentication.name, requestId)

        return RequestIdResponse(requestId)
    }
}
