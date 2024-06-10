package space.dawdawich.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import space.dawdawich.repositories.mongo.entity.SymbolInfoDocument

interface SymbolRepository : MongoRepository<SymbolInfoDocument, String> {

    @Query("{_id:  {\$eq:  ?0}}")
    @Update("{\$set:  {volatilityCoef:  ?1}}")
    fun updateVolatilityCoef(symbol: String, volatilityCoef: Double)
}
