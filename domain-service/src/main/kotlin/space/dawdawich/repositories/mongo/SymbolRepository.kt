package space.dawdawich.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.model.Market
import space.dawdawich.repositories.mongo.entity.SymbolDocument
import java.util.Optional

interface SymbolRepository : MongoRepository<SymbolDocument, String> {
    fun findByMarket(market: Market): List<SymbolDocument>
    fun findBySymbolAndMarket(symbol: String, market: Market): Optional<SymbolDocument>
}
