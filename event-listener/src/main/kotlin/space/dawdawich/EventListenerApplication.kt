package space.dawdawich

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import space.dawdawich.repositories.entity.GridTableAnalyzerDocument
import space.dawdawich.repositories.entity.SymbolInfoDocument
import java.util.UUID

@SpringBootApplication
open class EventListenerApplication

fun main() {
    runApplication<EventListenerApplication>()
}
