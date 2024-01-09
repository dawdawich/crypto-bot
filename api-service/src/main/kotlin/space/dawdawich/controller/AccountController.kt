package space.dawdawich.controller

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import space.dawdawich.configuration.provider.model.AccountDetails
import space.dawdawich.controller.model.AccountResponse
import space.dawdawich.controller.model.ApiTokenResponse
import space.dawdawich.controller.model.CreateAccountRequest
import space.dawdawich.controller.model.CreateApiTokenRequest
import space.dawdawich.service.AccountService

@RestController
@RequestMapping("/account")
class AccountController(private val accountService: AccountService) {

    @PostMapping
    fun createAccount(@RequestBody accRequest: CreateAccountRequest): ResponseEntity<Unit> {
        if (!accountService.isEmailAllowedToRegistration(accRequest.email.lowercase())) {
            return ResponseEntity(HttpStatus.PRECONDITION_FAILED)
        }
        if (accountService.isAccountAlreadyRegistered(accRequest.email.lowercase(), accRequest.username)) {
            return ResponseEntity(HttpStatus.CONFLICT)
        }
        accountService.fillAccountInfo(
                accRequest.email.lowercase(),
                accRequest.username,
                accRequest.name,
                accRequest.surname,
                accRequest.password)

        return ResponseEntity(HttpStatus.OK)
    }

    @GetMapping
    fun getAccount(authentication: Authentication): ResponseEntity<AccountResponse> {
        val accountDetails = authentication.principal as AccountDetails
        val account = accountService.getAccountById(accountDetails.accountId)
        val tokens =
            accountService.getTokens(account.id).map { ApiTokenResponse(it.id, it.apiKey, it.market.name, it.test) }
        return ResponseEntity.ok(
            AccountResponse(
                account.id,
                account.username,
                account.name,
                account.surname,
                account.email.lowercase(),
                account.createTime,
                tokens
            )
        )
    }

    @GetMapping("/api-token")
    fun getApiTokens(user: Authentication): ResponseEntity<List<ApiTokenResponse>> = ResponseEntity.ok(
        accountService.getApiTokens(user.name).map { ApiTokenResponse(it.id, it.apiKey, it.market.name, it.test) })

    @PostMapping("/api-token")
    fun addApiToken(@RequestBody request: CreateApiTokenRequest, user: Authentication): ResponseEntity<String> =
        ResponseEntity.ok(
            accountService.addApiToken(
                user.name,
                request.apiKey,
                request.secretKey,
                request.market,
                request.test
            )
        )

    @DeleteMapping("/api-token/{id}")
    fun deleteApiToken(@PathVariable("id") id: String, user: Authentication): ResponseEntity<Unit> {
        if (accountService.deleteApiToken(id, user.name) > 0) {
            return ResponseEntity(HttpStatus.OK)
        }
        return ResponseEntity(NOT_FOUND)
    }

    @GetMapping("/token")
    fun getJwtToken(
        @RequestHeader(
            HttpHeaders.AUTHORIZATION,
            required = true
        ) authorization: String
    ): ResponseEntity<String> {
        return try {
            val jwt = accountService.requestAccessToken(authorization)
            ResponseEntity.ok(jwt.toString())
        } catch (ex : BadCredentialsException){
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }
}
