package space.dawdawich.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import space.dawdawich.controller.model.ActivationRequest
import space.dawdawich.controller.model.TradeManagerRequest
import space.dawdawich.controller.model.TradeManagerResponse.Companion.convert
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.service.TradeManagerService

@RestController
@RequestMapping("/trade-manager")
class TradeManagerController(private val tradeMangerService: TradeManagerService) {

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    fun createNewTradeManager(user: Authentication, @RequestBody request: TradeManagerRequest) =
        tradeMangerService.createNewTraderManager(request.apiTokenId, request.active, request.customAnalyzerId, user.name)

    @GetMapping
    fun getAllManagers(user: Authentication) = tradeMangerService.findAllByAccountId(user.name).map { it.convert() }

    @GetMapping("/{managerId}")
    fun getManager(user: Authentication, @PathVariable managerId: String) =
        tradeMangerService.findManager(managerId, user.name)
            ?.let { return@let ResponseEntity(it.convert(), HttpStatus.OK) }
            ?: run { return@run ResponseEntity(HttpStatus.NOT_FOUND) }

    @PutMapping("/{managerId}/status")
    @ResponseStatus(HttpStatus.OK)
    fun changeManagerStatus(user: Authentication, @PathVariable managerId: String, @RequestBody request: ActivationRequest) = tradeMangerService.updateTradeManagerStatus(managerId, user.name, request.status)

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    fun updateManager(@RequestBody manager: TradeManagerDocument) = tradeMangerService.updateTradeManger(manager)
}
