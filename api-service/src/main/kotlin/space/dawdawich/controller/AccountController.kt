package space.dawdawich.controller

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import space.dawdawich.controller.model.ApiTokenResponse
import space.dawdawich.controller.model.CreateApiTokenRequest
import space.dawdawich.service.AccountService
import space.dawdawich.utils.baseDecode

@RestController
@RequestMapping("/account")
class AccountController(private val accountService: AccountService) {

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

    @GetMapping("/nonce")
    fun requestNonce(@RequestHeader("Account-Address") address: String): ResponseEntity<String> = try {
        ResponseEntity.ok(accountService.requestNonce(address.baseDecode()).toString())
    } catch (ex : IllegalArgumentException){
        ResponseEntity(HttpStatus.BAD_REQUEST)
    }
}
