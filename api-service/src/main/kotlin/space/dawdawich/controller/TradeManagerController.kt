package space.dawdawich.controller

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import space.dawdawich.controller.model.CreateTradeManagerRequest
import space.dawdawich.controller.model.TradeManagerResponse
import space.dawdawich.controller.model.TradeManagerResponse.Companion.convert
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.service.TradeManagerService

@RestController
@RequestMapping("/trade-manager")
class TradeManagerController(private val tradeManger: TradeManagerService) {

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    fun createNewTradeManager(user: Authentication, @RequestBody request: CreateTradeManagerRequest) = tradeManger.createNewTraderManager(request.tokenId, user.name)

    @GetMapping
    fun getAllManagers() = tradeManger.findAll().map { it.convert() }

    @GetMapping("/{managerId}")
    fun getManager(@PathVariable managerId: String) = tradeManger.findById(managerId).convert()

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    fun updateManager(@RequestBody manager: TradeManagerDocument) = tradeManger.updateTradeManger(manager)
}
