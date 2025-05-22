package space.dawdawich.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.mongo.entity.PriceTickModel

interface PriceTickRepository : MongoRepository<PriceTickModel, Int> {
    fun findAllByTimeIsGreaterThanAndPair(start: Long, pair: Int): List<PriceTickModel>
    fun findAllByTimeIsGreaterThan(start: Long): List<PriceTickModel>
    fun deleteByTimeIsLessThan(time: Long)
}
