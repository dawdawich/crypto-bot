package space.dawdawich.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import space.dawdawich.service.AccountService
import space.dawdawich.service.AccountTransactionService
import space.dawdawich.utils.baseDecode

@RestController
@RequestMapping("/account")
class AccountController(private val accountService: AccountService,
                        private val accountTransactionService: AccountTransactionService) {

    @GetMapping("/salt")
    fun requestSalt(@RequestHeader("Account-Address") address: String): ResponseEntity<String> = try {
        ResponseEntity.ok(accountService.requestSalt(address.baseDecode().lowercase()).toString())
    } catch (ex : IllegalArgumentException){
        ResponseEntity(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/transactions")
    fun getUserTransactions(user: Authentication) = accountTransactionService.getTransactions(user.name)
}
