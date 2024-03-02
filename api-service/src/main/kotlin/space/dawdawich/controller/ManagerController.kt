package space.dawdawich.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import space.dawdawich.controller.model.manager.ManagerRequest
import space.dawdawich.controller.model.manager.ManagerResponse.Companion.convert
import space.dawdawich.controller.model.manager.TradeManagerStatusRequest
import space.dawdawich.repositories.mongo.entity.TradeManagerDocument
import space.dawdawich.service.ManagerService

@RestController
@RequestMapping("/manager")
class ManagerController(private val mangerService: ManagerService) {

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    fun createNewManager(user: Authentication, @RequestBody request: ManagerRequest) =
        mangerService.createNewManager(request, user.name)

    @GetMapping
    fun getAllManagers(user: Authentication) = mangerService.findAllByAccountId(user.name).map { it }

    @GetMapping("/{managerId}")
    fun getManager(user: Authentication, @PathVariable managerId: String) =
        mangerService.findManager(managerId, user.name)
            ?.let { return@let ResponseEntity(it.convert(), HttpStatus.OK) }
            ?: run { return@run ResponseEntity(HttpStatus.NOT_FOUND) }

    @DeleteMapping("/{managerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteManager(user: Authentication, @PathVariable managerId: String): Unit =
        mangerService.deleteTradeManager(managerId, user.name)

    @PutMapping("/{managerId}/status")
    @ResponseStatus(HttpStatus.OK)
    fun changeManagerStatus(user: Authentication, @PathVariable managerId: String, @RequestBody request: TradeManagerStatusRequest) = mangerService.updateTradeManagerStatus(managerId, user.name, request.status)

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    fun updateManager(@RequestBody manager: TradeManagerDocument) = mangerService.updateTradeManger(manager)
}
