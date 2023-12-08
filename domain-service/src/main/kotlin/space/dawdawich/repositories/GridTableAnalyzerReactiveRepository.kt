package space.dawdawich.repositories

import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.mongodb.repository.Update
import reactor.core.publisher.Mono
import space.dawdawich.repositories.entity.GridTableAnalyzerDocument

interface GridTableAnalyzerReactiveRepository : ReactiveMongoRepository<GridTableAnalyzerDocument, String> {

    @Query("{_id : ?0}")
    @Update("{'\$set': { 'middlePrice': ?1 }}")
    fun updateMiddlePrice(id: String, money: Double)
}
