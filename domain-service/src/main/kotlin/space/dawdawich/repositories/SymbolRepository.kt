package space.dawdawich.repositories

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.entity.SymbolInfoDocument

interface SymbolRepository : MongoRepository<SymbolInfoDocument, ObjectId> {
    fun getBySymbolIs(symbol: String): SymbolInfoDocument
}
