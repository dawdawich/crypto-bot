package space.dawdawich.controller

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import space.dawdawich.controller.model.backtest.*
import space.dawdawich.service.BacktestService
import space.dawdawich.service.RequestStatusService
import java.util.*

@RestController
@RequestMapping("/backtest")
class BacktestController(
    private val backtestService: BacktestService,
    private val requestStatusService: RequestStatusService,
) {

    @GetMapping("/request/all", produces = [APPLICATION_JSON_VALUE])
    fun getBacktestsRequests(authentication: Authentication): List<RequestStatusResponse> = requestStatusService.getRequestStatusesForAccountId(authentication.name).reversed()

    @GetMapping("/request/{requestId}", produces = [APPLICATION_JSON_VALUE])
    fun getBacktestsDetails(authentication: Authentication, @PathVariable("requestId") requestId: String): ResponseEntity<BacktestRequestResultsResponse?> {
        if (!requestStatusService.isRequestIdExistForAccount(requestId, authentication.name)) {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity.ok(backtestService.getBackTestResults(requestId))
    }

//    @PostMapping(consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
//    @ResponseStatus(HttpStatus.CREATED)
//    fun createBacktest(@RequestBody request: BacktestRequest, authentication: Authentication): RequestIdResponse {
//        val requestId = UUID.randomUUID().toString()
//        backtestService.createBacktest(request, authentication.name, requestId)
//
//        return RequestIdResponse(requestId)
//    }

    @PostMapping(consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun createPredefinedBacktest(@RequestBody request: PredefinedBacktestRequest, authentication: Authentication): RequestIdResponse {
        val requestId = UUID.randomUUID().toString()
        backtestService.createPredefinedBacktest(request, authentication.name, requestId)

        return RequestIdResponse(requestId)
    }
}
