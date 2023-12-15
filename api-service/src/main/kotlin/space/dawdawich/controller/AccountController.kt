package space.dawdawich.controller

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import space.dawdawich.configuration.provider.model.AccountDetails
import space.dawdawich.controller.model.AccountResponse
import space.dawdawich.controller.model.CreateAccountRequest
import space.dawdawich.service.AccountService

@RestController
@RequestMapping("/account")
class AccountController(private val accountService: AccountService) {

    @PostMapping
    fun createAccount(@RequestBody accRequest: CreateAccountRequest): ResponseEntity<Unit> { // TODO: Add checks that account did not registered
        if (accountService.isAccountEmailExist(accRequest.email)) {
            accountService.fillAccountInfo(accRequest.email, accRequest.username, accRequest.name, accRequest.surname, accRequest.password)
            return ResponseEntity.ok().build()
        }
        return ResponseEntity(HttpStatus.PRECONDITION_FAILED)
    }

    @GetMapping
    fun getAccount(authentication: Authentication): ResponseEntity<AccountResponse> {
        val accountDetails = authentication.principal as AccountDetails
        val account = accountService.getAccountById(accountDetails.accountId)
        return ResponseEntity.ok(
            AccountResponse(account.id, account.username, account.name, account.surname, account.email, authentication.authorities.toList()[0].authority)
        )
    }

    @GetMapping("/token")
    fun getJwtToken(@RequestHeader(HttpHeaders.AUTHORIZATION, required = true) authorization: String): ResponseEntity<Unit> {
        val jwt = accountService.requestAccessToken(authorization)
        val headers = HttpHeaders().apply { this[HttpHeaders.AUTHORIZATION] = listOf(jwt.toString()) }
        return ResponseEntity.ok().headers(headers).build()
    }
}
