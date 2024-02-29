package space.dawdawich.controller

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import space.dawdawich.controller.model.account.api_token.ApiTokenResponse
import space.dawdawich.controller.model.account.api_token.CreateApiTokenRequest
import space.dawdawich.service.AccountService
import space.dawdawich.service.AccountTransactionService
import space.dawdawich.utils.baseDecode

@RestController
@RequestMapping("/account")
class AccountController(private val accountService: AccountService, private val accountTransactionService: AccountTransactionService) {

    @GetMapping("/api-token")
    fun getApiTokens(user: Authentication): ResponseEntity<List<ApiTokenResponse>> = ResponseEntity.ok(
        accountService.getApiTokens(user.name).map { ApiTokenResponse(it.id, it.apiKey, it.market.name, it.demoAccount) })

    @PostMapping("/api-token")
    fun addApiToken(@RequestBody request: CreateApiTokenRequest, user: Authentication): ResponseEntity<String> =
        ResponseEntity.ok(
            accountService.addApiToken(
                user.name,
                request.apiKey,
                request.secretKey,
                request.market,
                request.demo
            )
        )

    @DeleteMapping("/api-token/{id}")
    fun deleteApiToken(@PathVariable("id") id: String, user: Authentication): ResponseEntity<Unit> {
        if (accountService.deleteApiToken(id, user.name) > 0) {
            return ResponseEntity(HttpStatus.OK)
        }
        return ResponseEntity(NOT_FOUND)
    }

    @GetMapping("/salt")
    fun requestSalt(@RequestHeader("Account-Address") address: String): ResponseEntity<String> = try {
        ResponseEntity.ok(accountService.requestSalt(address.baseDecode().lowercase()).toString())
    } catch (ex : IllegalArgumentException){
        ResponseEntity(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/transactions")
    fun getUserTransactions(user: Authentication) = accountTransactionService.getTransactions(user.name)
}
