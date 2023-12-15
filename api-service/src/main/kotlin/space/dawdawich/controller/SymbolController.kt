package space.dawdawich.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import space.dawdawich.controller.model.CreateSymbolRequest
import space.dawdawich.controller.model.SymbolResponse
import space.dawdawich.service.SymbolService

@RestController
@RequestMapping("/symbol")
class SymbolController(private val symbolService: SymbolService) {

    @GetMapping("/names")
    fun getAllSymbolsName(): List<String> = symbolService.getAllSymbolsName()

    @GetMapping
    fun getAllSymbols(): List<SymbolResponse> = symbolService.getAllSymbols()

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    fun addNewSymbolToList(@RequestBody request: CreateSymbolRequest) {
        symbolService.addNewSymbol(request.symbol, request.isOneWayMode, request.priceMinStep)
    }
}
