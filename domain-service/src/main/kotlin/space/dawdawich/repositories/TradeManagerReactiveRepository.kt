package space.dawdawich.repositories

import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.mongodb.repository.Tailable
import org.springframework.data.mongodb.repository.Update
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import space.dawdawich.repositories.entity.TradeManagerDocument

interface TradeManagerReactiveRepository: ReactiveMongoRepository<TradeManagerDocument, String> {
    @Query("{_id: ?0}")
    fun getById(managerId: String): Mono<TradeManagerDocument>

    @Tailable
    fun findWithTailableCursorById(id: String): Flux<TradeManagerDocument>
}
