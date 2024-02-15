package space.dawdawich.repositories.mongo

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.mongo.entity.SymbolInfoDocument

interface SymbolRepository : MongoRepository<SymbolInfoDocument, String> {
    fun getBySymbolIs(symbol: String): SymbolInfoDocument
}
