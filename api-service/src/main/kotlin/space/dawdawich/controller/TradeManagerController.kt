package space.dawdawich.controller

import org.springframework.web.bind.annotation.*
import space.dawdawich.repositories.entity.TradeManagerDocument
import space.dawdawich.service.TradeManagerService

@RestController
@RequestMapping("/trade-manager")
class TradeManagerController(private val tradeManger: TradeManagerService) {

    @PostMapping
    fun createNewTradeManager() = "id" to tradeManger.createNewTraderManager()

    @GetMapping
    fun getAllManagers() = tradeManger.findAll()

    @GetMapping("/{managerId}")
    fun getManager(@PathVariable managerId: String) = tradeManger.findById(managerId)

    @PutMapping
    fun updateManager(@RequestBody manager: TradeManagerDocument) = tradeManger.updateTradeManger(manager)
}
