package space.dawdawich.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import space.dawdawich.repositories.mongo.entity.SymbolDocument

interface SymbolRepository : MongoRepository<SymbolDocument, String>
